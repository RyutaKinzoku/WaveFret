package com.example.wavefret.practice.playalong

/** The original song during play-along or listening: a stored audio file or a YouTube video. */
interface PlayAlongMediaPlayer {

    /** Latest known position of the song, in milliseconds. Type: Long */
    val positionMillis: Long

    /** True while the song is playing. Type: Boolean */
    val isPlaying: Boolean

    /**
     * Starts or resumes the song.
     *
     * @return Unit
     */
    fun play()

    /**
     * Pauses the song where it is.
     *
     * @return Unit
     */
    fun pause()

    /**
     * @param positionMillis Where to jump to. Type: Long
     * @return Unit
     */
    fun seekTo(positionMillis: Long)

    /**
     * Frees the player; it must not be used afterwards.
     *
     * @return Unit
     */
    fun release()
}
