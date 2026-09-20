package com.example.wavefret.recording

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingDisplayFormatterTest {

    @Test
    fun formatCombinesFileNameAndFormattedDate() {
        val formatter = RecordingDisplayFormatter(FakeDateFormatter(fixedOutput = "Jan 1, 2024 10:00"))
        val recording = RecordingInfo(
            filePath = "/fake/dir/recording_1.m4a",
            fileName = "recording_1.m4a",
            lastModifiedMillis = 123L
        )
        assertEquals("recording_1.m4a  •  Jan 1, 2024 10:00", formatter.format(recording))
    }
}