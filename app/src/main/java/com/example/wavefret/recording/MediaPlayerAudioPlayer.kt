package com.example.wavefret.recording

import android.media.MediaPlayer
import android.util.Log

/** Real AudioPlayer implementation backed by android.media.MediaPlayer. */
class MediaPlayerAudioPlayer : AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null

    /**
     * @param filePath Absolute path of the audio file to play. Type: String
     * @param onCompleted Invoked when playback reaches the end of the file on its own. Type: () -> Unit
     * @return Unit
     */
    override fun playFile(filePath: String, onCompleted: () -> Unit) {
        try {
            releasePlayer()
            val player = MediaPlayer()
            player.setDataSource(filePath)
            player.setOnCompletionListener { onCompleted() }
            player.prepare()
            player.start()
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play $filePath", e)
        }
    }

    /** @return Unit */
    override fun stopPlayback() {
        try {
            mediaPlayer?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop playback", e)
        }
    }

    /** @return Unit */
    override fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    companion object {
        private const val TAG = "MediaPlayerAudioPlayer"
    }
}