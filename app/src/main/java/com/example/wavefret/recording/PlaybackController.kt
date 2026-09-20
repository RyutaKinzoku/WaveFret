package com.example.wavefret.recording

/**
 * Orchestrates playback of recordings: which file (if any) is currently
 * playing, and starting/stopping/toggling playback. Single responsibility:
 * playback session bookkeeping, independent of the UI and the concrete
 * audio-playback technology.
 *
 * @property audioPlayer Platform player abstraction to start/stop. Type: AudioPlayer
 * @property onPlaybackStateChanged Invoked whenever the currently-playing file changes, with the previous and new file paths, so the UI can refresh just those rows. Type: (String?, String?) -> Unit
 */
class PlaybackController(
    private val audioPlayer: AudioPlayer,
    private val onPlaybackStateChanged: (previousFilePath: String?, currentFilePath: String?) -> Unit = { _, _ -> }
) {

    private var currentlyPlayingFilePath: String? = null

    /** @return Absolute path of the file currently playing, or null if nothing is playing. Type: String? */
    fun currentlyPlayingFilePath(): String? = currentlyPlayingFilePath

    /**
     * @param filePath File to check. Type: String
     * @return True if filePath is the one currently playing. Type: Boolean
     */
    fun isPlaying(filePath: String): Boolean = currentlyPlayingFilePath == filePath

    /**
     * Handles a tap on a recording's play/stop button: stops playback if the
     * same file was already playing, otherwise switches directly to this file
     * (stopping any other playback first) as a single atomic state change.
     *
     * @param filePath Absolute path of the tapped recording. Type: String
     * @return Unit
     */
    fun onItemClicked(filePath: String) {
        if (currentlyPlayingFilePath == filePath) {
            stopPlayback()
            return
        }
        if (currentlyPlayingFilePath != null) {
            audioPlayer.stopPlayback()
            audioPlayer.releasePlayer()
        }
        setCurrentlyPlaying(filePath)
        audioPlayer.playFile(filePath) { onPlaybackCompleted() }
    }

    /**
     * Stops playback if something is currently playing; has no effect if idle.
     *
     * @return Unit
     */
    fun stopPlayback() {
        if (currentlyPlayingFilePath == null) return
        audioPlayer.stopPlayback()
        audioPlayer.releasePlayer()
        setCurrentlyPlaying(null)
    }

    /**
     * Called when the platform player reaches the end of the file on its own.
     *
     * @return Unit
     */
    private fun onPlaybackCompleted() {
        audioPlayer.releasePlayer()
        setCurrentlyPlaying(null)
    }

    /**
     * @param filePath New currently-playing file path, or null. Type: String?
     * @return Unit
     */
    private fun setCurrentlyPlaying(filePath: String?) {
        val previousFilePath = currentlyPlayingFilePath
        currentlyPlayingFilePath = filePath
        onPlaybackStateChanged(previousFilePath, filePath)
    }
}