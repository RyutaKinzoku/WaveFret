package com.example.wavefret.practice.playalong

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeLinkParserTest {

    private val parser = YouTubeLinkParser()

    @Test
    fun watchLinks() {
        assertEquals("dQw4w9WgXcQ", parser.videoIdFrom("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", parser.videoIdFrom("https://m.youtube.com/watch?feature=share&v=dQw4w9WgXcQ&t=42"))
        assertEquals("dQw4w9WgXcQ", parser.videoIdFrom("https://music.youtube.com/watch?v=dQw4w9WgXcQ&list=RD"))
    }

    @Test
    fun shortLinksShortsAndEmbeds() {
        assertEquals("dQw4w9WgXcQ", parser.videoIdFrom("https://youtu.be/dQw4w9WgXcQ?si=abc"))
        assertEquals("dQw4w9WgXcQ", parser.videoIdFrom("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", parser.videoIdFrom("https://www.youtube.com/embed/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", parser.videoIdFrom("youtube.com/live/dQw4w9WgXcQ"))
    }

    @Test
    fun linkInsideSharedTextIsFound() {
        assertEquals("dQw4w9WgXcQ", parser.videoIdFrom("Check this bass line! https://youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun otherSitesAndBrokenIdsAreNotYouTube() {
        assertEquals(null, parser.videoIdFrom("https://vimeo.com/123456"))
        assertEquals(null, parser.videoIdFrom("https://www.youtube.com/watch?v=short"))
        assertEquals(null, parser.videoIdFrom("G|---3---|"))
    }
}
