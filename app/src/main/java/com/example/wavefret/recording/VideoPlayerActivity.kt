package com.example.wavefret.recording

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import com.example.wavefret.R
import com.example.wavefret.common.ui.WaveFretActivity
import com.example.wavefret.common.ui.applySystemBarPadding
import com.google.android.material.appbar.MaterialToolbar

/**
 * Plays one video take full screen with standard playback controls.
 *
 * Android framework glue with no logic, verified on-device.
 */
class VideoPlayerActivity : WaveFretActivity() {

    private lateinit var videoView: VideoView

    /**
     * @param savedInstanceState Previously saved state, or null on first creation. Type: Bundle?
     * @return Unit
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_video_player)
        findViewById<View>(R.id.videoPlayerRoot).applySystemBarPadding(applyTop = true, applyBottom = true)
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.title = intent.getStringExtra(EXTRA_TITLE)
        topAppBar.setNavigationOnClickListener { finish() }

        videoView = findViewById(R.id.videoPlayerVideoView)
        videoView.setMediaController(MediaController(this).also { it.setAnchorView(videoView) })
        videoView.setVideoPath(intent.getStringExtra(EXTRA_FILE_PATH))
        videoView.setOnPreparedListener { videoView.start() }
    }

    /**
     * @return Unit
     */
    override fun onPause() {
        super.onPause()
        videoView.pause()
    }

    companion object {
        private const val EXTRA_FILE_PATH = "com.example.wavefret.recording.VIDEO_FILE_PATH"
        private const val EXTRA_TITLE = "com.example.wavefret.recording.VIDEO_TITLE"

        /**
         * @param context Context starting the player. Type: Context
         * @param filePath Video file to play. Type: String
         * @param title Title shown in the app bar. Type: String
         * @return An intent that plays the video. Type: Intent
         */
        fun intentFor(context: Context, filePath: String, title: String): Intent {
            return Intent(context, VideoPlayerActivity::class.java)
                .putExtra(EXTRA_FILE_PATH, filePath)
                .putExtra(EXTRA_TITLE, title)
        }
    }
}
