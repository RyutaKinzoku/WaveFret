package com.example.wavefret.recording

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingFileNamerTest {

    @Test
    fun generateFileNameIncludesCurrentTimeAndM4aExtension() {
        val fileNamer = RecordingFileNamer(clock = FakeClock(fixedTimeMillis = 1_700_000_000_000L))
        assertEquals("recording_1700000000000.m4a", fileNamer.generateFileName())
    }

    @Test
    fun generateFileNameReflectsADifferentFixedTime() {
        val fileNamer = RecordingFileNamer(clock = FakeClock(fixedTimeMillis = 42L))
        assertEquals("recording_42.m4a", fileNamer.generateFileName())
    }
}