package com.example.wavefret.practice.playalong

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView

/**
 * Plays a YouTube video through YouTube's official embedded IFrame Player
 * API inside a WebView, as YouTube's terms allow: the video is streamed and
 * shown by YouTube's own player, never downloaded. The song position is
 * polled from the player every [POSITION_POLL_MILLIS].
 *
 * Thin wrapper over WebView, verified on-device. The video id is checked
 * against YouTube's id format before it is placed in the page.
 *
 * @param webView WebView to show the player in. Type: WebView
 * @param videoId 11-character YouTube video id. Type: String
 * @param onReady Called once the player can be controlled. Type: () -> Unit
 * @param onError Called with YouTube's error code (e.g. 101/150: embedding not allowed). Type: (Int) -> Unit
 */
@SuppressLint("SetJavaScriptEnabled")
class YouTubePlayAlongPlayer(
    private val webView: WebView,
    videoId: String,
    private val onReady: () -> Unit,
    private val onError: (Int) -> Unit
) : PlayAlongMediaPlayer {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var latestPositionMillis = 0L
    private var isReady = false
    private var latestIsPlaying = false

    private val positionPoller = object : Runnable {
        override fun run() {
            webView.evaluateJavascript("wfPosition()") { positionText ->
                positionText?.trim('"')?.toLongOrNull()?.takeIf { it >= 0 }?.let { latestPositionMillis = it }
            }
            mainHandler.postDelayed(this, POSITION_POLL_MILLIS)
        }
    }

    init {
        require(videoId.matches(VIDEO_ID_FORMAT)) { "Not a YouTube video id" }
        webView.settings.javaScriptEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.addJavascriptInterface(PlayerEvents(), JAVASCRIPT_BRIDGE_NAME)
        webView.loadDataWithBaseURL(EMBED_ORIGIN, playerPage(videoId), "text/html", "utf-8", null)
    }

    /** Latest polled position, in milliseconds. Type: Long */
    override val positionMillis: Long get() = latestPositionMillis

    /** True while YouTube reports the video playing. Type: Boolean */
    override val isPlaying: Boolean get() = latestIsPlaying

    /** @return Unit */
    override fun play() {
        if (isReady) webView.evaluateJavascript("player.playVideo()", null)
    }

    /** @return Unit */
    override fun pause() {
        if (isReady) webView.evaluateJavascript("player.pauseVideo()", null)
    }

    /**
     * @param positionMillis Where to jump to. Type: Long
     * @return Unit
     */
    override fun seekTo(positionMillis: Long) {
        if (isReady) webView.evaluateJavascript("player.seekTo(${positionMillis / MILLIS_PER_SECOND.toDouble()}, true)", null)
        latestPositionMillis = positionMillis
    }

    /** @return Unit */
    override fun release() {
        mainHandler.removeCallbacks(positionPoller)
        webView.removeJavascriptInterface(JAVASCRIPT_BRIDGE_NAME)
        webView.loadUrl("about:blank")
    }

    /** Receives player events from the page's JavaScript (on a background thread). */
    private inner class PlayerEvents {

        /** @return Unit */
        @JavascriptInterface
        fun onReady() {
            mainHandler.post {
                isReady = true
                mainHandler.post(positionPoller)
                this@YouTubePlayAlongPlayer.onReady()
            }
        }

        /**
         * @param playerState YouTube player state (1 = playing). Type: Int
         * @return Unit
         */
        @JavascriptInterface
        fun onStateChange(playerState: Int) {
            mainHandler.post { latestIsPlaying = playerState == YOUTUBE_STATE_PLAYING }
        }

        /**
         * @param errorCode YouTube error code. Type: Int
         * @return Unit
         */
        @JavascriptInterface
        fun onError(errorCode: Int) {
            mainHandler.post { this@YouTubePlayAlongPlayer.onError(errorCode) }
        }
    }

    private companion object {
        /** YouTube asks embedding apps to identify themselves with an https://<package> origin. */
        const val EMBED_ORIGIN = "https://com.example.wavefret"
        const val JAVASCRIPT_BRIDGE_NAME = "WaveFret"
        const val POSITION_POLL_MILLIS = 50L
        const val MILLIS_PER_SECOND = 1_000L
        const val YOUTUBE_STATE_PLAYING = 1
        val VIDEO_ID_FORMAT = Regex("[A-Za-z0-9_-]{11}")

        /**
         * @param videoId Validated video id. Type: String
         * @return HTML hosting the IFrame player and the position helper. Type: String
         */
        fun playerPage(videoId: String) = """
            <!DOCTYPE html><html><head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>html,body{margin:0;height:100%;background:#000}#player{width:100%;height:100%}</style>
            </head><body><div id="player"></div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
            var player;
            function onYouTubeIframeAPIReady() {
              player = new YT.Player('player', {
                videoId: '$videoId',
                playerVars: { playsinline: 1, rel: 0, origin: '$EMBED_ORIGIN' },
                events: {
                  onReady: function() { WaveFret.onReady(); },
                  onStateChange: function(event) { WaveFret.onStateChange(event.data); },
                  onError: function(event) { WaveFret.onError(event.data); }
                }
              });
            }
            function wfPosition() {
              return (player && player.getCurrentTime) ? Math.round(player.getCurrentTime() * 1000) : -1;
            }
            </script></body></html>
        """.trimIndent()
    }
}
