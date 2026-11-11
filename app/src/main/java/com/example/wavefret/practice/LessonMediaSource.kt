package com.example.wavefret.practice

/** The recording a timed lesson was made from, used for play-along. */
sealed class LessonMediaSource {

    /**
     * A YouTube video, played in YouTube's embedded player.
     *
     * @property videoId The 11-character video id. Type: String
     */
    data class YouTubeVideo(val videoId: String) : LessonMediaSource()

    /**
     * An audio file kept in the app's lesson-audio folder.
     *
     * @property fileName Name of the stored file. Type: String
     */
    data class StoredAudio(val fileName: String) : LessonMediaSource()
}
