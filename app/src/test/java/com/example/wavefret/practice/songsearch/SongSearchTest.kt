package com.example.wavefret.practice.songsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SongSearchTest {

    private val songSearchLinks = SongSearchLinks()

    @Test
    fun youTubeSearchLooksForBassCovers() {
        assertEquals(
            "https://www.youtube.com/results?search_query=Billie+Jean+bass+cover",
            songSearchLinks.youTubeBassCoverSearch("Billie Jean")
        )
    }

    @Test
    fun webSearchLooksForBassTabs() {
        assertEquals(
            "https://www.google.com/search?q=Billie+Jean+bass+tab",
            songSearchLinks.webBassTabSearch("  Billie Jean ")
        )
    }

    @Test
    fun specialCharactersAreEncoded() {
        assertEquals(
            "https://www.google.com/search?q=AC%2FDC+%26+Friends+bass+tab",
            songSearchLinks.webBassTabSearch("AC/DC & Friends")
        )
    }

    @Test
    fun aBlankSongNameIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { songSearchLinks.youTubeBassCoverSearch("  ") }
    }

    @Test
    fun aPendingSongTitleIsUsedWithinTheHour() {
        val pendingSongTitle = PendingSongTitle("Billie Jean", searchedAtMillis = 1_000)

        assertTrue(pendingSongTitle.isFreshAt(1_000 + 59 * 60_000))
        assertFalse(pendingSongTitle.isFreshAt(1_000 + 61 * 60_000))
    }
}
