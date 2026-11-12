package com.example.wavefret.practice.songsearch

import java.net.URLEncoder

/**
 * Builds the searches behind "Find a song": a YouTube search for bass
 * covers (shared back to WaveFret, it becomes a lesson through Listen mode)
 * and a web search for bass tabs (a copied or shared tab is imported). The
 * player browses the results themselves; WaveFret downloads nothing.
 */
class SongSearchLinks {

    /**
     * @param songName What the player typed, e.g. "Billie Jean". Type: String
     * @return A YouTube search URL for bass covers of it. Type: String
     */
    fun youTubeBassCoverSearch(songName: String): String = YOUTUBE_SEARCH + encoded("$songName $BASS_COVER")

    /**
     * @param songName What the player typed. Type: String
     * @return A web search URL for bass tabs of it. Type: String
     */
    fun webBassTabSearch(songName: String): String = WEB_SEARCH + encoded("$songName $BASS_TAB")

    /**
     * @param query Search words. Type: String
     * @return The words, trimmed and URL-encoded. Type: String
     * @throws IllegalArgumentException If no song name was given.
     */
    private fun encoded(query: String): String {
        val songWords = query.trim()
        require(songWords.removeSuffix(BASS_COVER).removeSuffix(BASS_TAB).isNotBlank()) { "Type a song name first" }
        return URLEncoder.encode(songWords.replace(Regex("\\s+"), " "), Charsets.UTF_8.name())
    }

    private companion object {
        const val YOUTUBE_SEARCH = "https://www.youtube.com/results?search_query="
        const val WEB_SEARCH = "https://www.google.com/search?q="
        const val BASS_COVER = "bass cover"
        const val BASS_TAB = "bass tab"
    }
}
