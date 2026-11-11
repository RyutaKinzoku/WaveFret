package com.example.wavefret.practice.playalong

import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File

/**
 * Plays a stored lesson audio file with [MediaPlayer]. Thin framework
 * wrapper, verified on-device.
 *
 * @param audioFile File to play. Type: File
 * @param onReady Called once the file is ready to play. Type: () -> Unit
 * @param onError Called if the file can't be played. Type: () -> Unit
 */
class AudioFilePlayAlongPlayer(audioFile: File, onReady: () -> Unit, onError: () -> Unit) : PlayAlongMediaPlayer {

    private val mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
        )
        setOnErrorListener { _, _, _ -> onError(); true }
        setDataSource(audioFile.absolutePath)
        prepareAsync()
    }
    private var isPrepared = false

    init {
        mediaPlayer.setOnPreparedListener {
            isPrepared = true
            onReady()
        }
    }

    /** Current position, in milliseconds. Type: Long */
    override val positionMillis: Long get() = if (isPrepared) mediaPlayer.currentPosition.toLong() else 0L

    /** True while playing. Type: Boolean */
    override val isPlaying: Boolean get() = isPrepared && mediaPlayer.isPlaying

    /** @return Unit */
    override fun play() {
        if (isPrepared && !mediaPlayer.isPlaying) mediaPlayer.start()
    }

    /** @return Unit */
    override fun pause() {
        if (isPrepared && mediaPlayer.isPlaying) mediaPlayer.pause()
    }

    /**
     * @param positionMillis Where to jump to. Type: Long
     * @return Unit
     */
    override fun seekTo(positionMillis: Long) {
        if (isPrepared) mediaPlayer.seekTo(positionMillis.toInt())
    }

    /** @return Unit */
    override fun release() = mediaPlayer.release()
}
