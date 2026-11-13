package com.example.wavefret.practice.songsearch

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import com.example.wavefret.R
import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.ui.WaveFretActivity
import com.example.wavefret.common.ui.applySystemBarPadding
import com.example.wavefret.practice.playalong.ListenActivity
import com.example.wavefret.practice.playalong.YouTubeLinkParser
import com.example.wavefret.practice.songimport.AsciiTabParser
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import org.json.JSONException

/**
 * Finds a song without leaving WaveFret: YouTube bass covers or web bass
 * tabs are searched in a built-in browser. When a video is open, "Use this
 * video" takes it to Listen mode; when a tab page is open, "Use the tab on
 * this page" reads the tab text the page shows (no download or account
 * needed) and hands it back to the library.
 *
 * Android framework glue; the tab is cut out by the tested
 * [AsciiTabParser.extractTab] and videos are recognised by the tested
 * [YouTubeLinkParser].
 */
class SongFinderActivity : WaveFretActivity() {

    /** What the player is looking for. */
    enum class SearchKind {
        /** YouTube bass covers. */
        VIDEO,

        /** Bass tabs on the web. */
        TAB
    }

    private lateinit var rootView: View
    private lateinit var webView: WebView
    private lateinit var hintTextView: TextView
    private lateinit var useButton: MaterialButton
    private lateinit var progressIndicator: LinearProgressIndicator

    private val songSearchLinks = SongSearchLinks()
    private val youTubeLinkParser = YouTubeLinkParser()
    private val asciiTabParser = AsciiTabParser(BassTunings.STANDARD)
    private var songTitle = ""
    private var searchKind = SearchKind.VIDEO

    /**
     * @param savedInstanceState Previously saved state, or null on first creation. Type: Bundle?
     * @return Unit
     */
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_song_finder)
        rootView = findViewById(R.id.songFinderRoot)
        rootView.applySystemBarPadding(applyTop = true, applyBottom = true)
        webView = findViewById(R.id.songFinderWebView)
        hintTextView = findViewById(R.id.songFinderHintTextView)
        useButton = findViewById(R.id.songFinderUseButton)
        progressIndicator = findViewById(R.id.songFinderProgressIndicator)

        songTitle = intent.getStringExtra(EXTRA_SONG_TITLE).orEmpty()
        searchKind = IntentCompat.getSerializableExtra(intent, EXTRA_SEARCH_KIND, SearchKind::class.java) ?: SearchKind.VIDEO

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.title = songTitle
        topAppBar.setSubtitle(R.string.song_finder_subtitle)
        topAppBar.setNavigationOnClickListener { finish() }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = SongFinderWebViewClient()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })

        val kindToggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.songFinderKindToggleGroup)
        kindToggleGroup.check(if (searchKind == SearchKind.VIDEO) R.id.songFinderVideosButton else R.id.songFinderTabsButton)
        kindToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                searchKind = if (checkedId == R.id.songFinderVideosButton) SearchKind.VIDEO else SearchKind.TAB
                startSearch()
            }
        }
        useButton.setOnClickListener { onUseClicked() }
        startSearch()
    }

    /**
     * @return Unit
     */
    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    /**
     * @return Unit
     */
    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    /**
     * @return Unit
     */
    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    /**
     * Loads the search for the current kind and resets the bottom panel.
     *
     * @return Unit
     */
    private fun startSearch() {
        val searchUrl = runCatching {
            if (searchKind == SearchKind.VIDEO) songSearchLinks.youTubeBassCoverSearch(songTitle)
            else songSearchLinks.webBassTabSearch(songTitle)
        }.getOrNull() ?: return finish()
        webView.loadUrl(searchUrl)
        refreshBottomPanel(searchUrl)
    }

    /**
     * Updates the hint and the use button for the page now shown.
     *
     * @param pageUrl Address of the page now shown. Type: String?
     * @return Unit
     */
    private fun refreshBottomPanel(pageUrl: String?) {
        if (searchKind == SearchKind.VIDEO) {
            val isVideoOpen = pageUrl?.let(youTubeLinkParser::videoIdFrom) != null
            useButton.setText(R.string.song_finder_use_video)
            useButton.isEnabled = isVideoOpen
            hintTextView.setText(if (isVideoOpen) R.string.song_finder_video_ready_hint else R.string.song_finder_videos_hint)
        } else {
            useButton.setText(R.string.song_finder_use_tab)
            useButton.isEnabled = true
            hintTextView.setText(R.string.song_finder_tabs_hint)
        }
    }

    /**
     * @return Unit
     */
    private fun onUseClicked() {
        if (searchKind == SearchKind.VIDEO) useOpenVideo() else useTabOnPage()
    }

    /**
     * Stops the video here and opens it in Listen mode.
     *
     * @return Unit
     */
    private fun useOpenVideo() {
        val videoId = webView.url?.let(youTubeLinkParser::videoIdFrom) ?: return
        webView.loadUrl(BLANK_PAGE)
        startActivity(ListenActivity.intentFor(this, videoId, songTitle = songTitle))
        finish()
    }

    /**
     * Reads the text the page shows and returns the tab in it, if any.
     *
     * @return Unit
     */
    private fun useTabOnPage() {
        webView.evaluateJavascript(PAGE_TEXT_SCRIPT) { encodedPageText ->
            val pageText = try {
                JSONArray("[$encodedPageText]").optString(0)
            } catch (unreadablePage: JSONException) {
                ""
            }
            val tabText = asciiTabParser.extractTab(pageText)
            if (tabText == null) {
                Snackbar.make(rootView, R.string.song_finder_no_tab_on_page, Snackbar.LENGTH_LONG).setAnchorView(useButton).show()
            } else {
                setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_TAB_TEXT, tabText).putExtra(EXTRA_SONG_TITLE, songTitle))
                finish()
            }
        }
    }

    /**
     * Keeps web pages inside the finder, ignores links to other apps, and
     * follows YouTube's in-page navigation to know when a video is open.
     */
    private inner class SongFinderWebViewClient : WebViewClient() {

        /**
         * @param view The finder's browser. Type: WebView
         * @param request Navigation about to happen. Type: WebResourceRequest
         * @return True to block links that aren't web pages. Type: Boolean
         */
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val scheme = request.url.scheme
            return scheme != "http" && scheme != "https"
        }

        /**
         * @param view The finder's browser. Type: WebView
         * @param url Page starting to load. Type: String?
         * @param favicon Page icon, unused. Type: Bitmap?
         * @return Unit
         */
        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            progressIndicator.visibility = View.VISIBLE
        }

        /**
         * @param view The finder's browser. Type: WebView
         * @param url Page that finished loading. Type: String?
         * @return Unit
         */
        override fun onPageFinished(view: WebView, url: String?) {
            progressIndicator.visibility = View.INVISIBLE
            refreshBottomPanel(url)
        }

        /**
         * @param view The finder's browser. Type: WebView
         * @param url New address, including in-page changes on YouTube. Type: String?
         * @param isReload True if the page was reloaded. Type: Boolean
         * @return Unit
         */
        override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
            refreshBottomPanel(url)
        }
    }

    companion object {
        /** Extra holding the tab text the finder returns. Type: String */
        const val EXTRA_TAB_TEXT = "com.example.wavefret.extra.FOUND_TAB_TEXT"

        /** Extra holding the searched song name. Type: String */
        const val EXTRA_SONG_TITLE = "com.example.wavefret.extra.FOUND_SONG_TITLE"

        private const val EXTRA_SEARCH_KIND = "com.example.wavefret.extra.SEARCH_KIND"
        private const val BLANK_PAGE = "about:blank"
        private const val PAGE_TEXT_SCRIPT = "(function() { return document.body ? document.body.innerText : ''; })()"

        /**
         * @param context Context used to build the intent. Type: Context
         * @param songTitle Song to search for. Type: String
         * @param searchKind Videos or tabs. Type: SearchKind
         * @return Intent that opens the finder. Type: Intent
         */
        fun intentFor(context: Context, songTitle: String, searchKind: SearchKind): Intent =
            Intent(context, SongFinderActivity::class.java)
                .putExtra(EXTRA_SONG_TITLE, songTitle)
                .putExtra(EXTRA_SEARCH_KIND, searchKind)
    }
}
