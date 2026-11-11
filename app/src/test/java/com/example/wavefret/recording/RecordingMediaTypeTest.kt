package com.example.wavefret.recording

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingMediaTypeTest {

    @Test
    fun audioTakesAreSharedAsMp4Audio() {
        assertEquals("audio/mp4", RecordingMediaType.AUDIO.shareMimeType)
    }

    @Test
    fun videoTakesAreSharedAsMp4Video() {
        assertEquals("video/mp4", RecordingMediaType.VIDEO.shareMimeType)
    }

    @Test
    fun typeIsWorkedOutFromTheFileExtension() {
        assertEquals(RecordingMediaType.VIDEO, RecordingMediaType.forFileName("take.MP4"))
        assertEquals(RecordingMediaType.AUDIO, RecordingMediaType.forFileName("take.m4a"))
        assertEquals(null, RecordingMediaType.forFileName("take.txt"))
    }
}
