package com.example.wavefret.recording

/**
 * Abstraction over the platform audio-playback API, so playback
 * orchestration logic can be unit tested without an Android framework
 * dependency or real audio hardware.
 */
interface AudioPlayer {
    /**
     * @param filePath Absolute path of the audio file to play. Type: String
     * @param onCompleted Invoked when playback reaches the end of the file on its own. Type: () -> Unit
     * @return Unit
     */
    fun playFile(filePath: String, onCompleted: () -> Unit)

    /** @return Unit */
    fun stopPlayback()

    /** @return Unit */
    fun releasePlayer()
}