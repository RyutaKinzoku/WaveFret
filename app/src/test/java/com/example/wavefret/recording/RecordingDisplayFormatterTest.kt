package com.example.wavefret.recording

import com.example.wavefret.common.time.DurationFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingDisplayFormatterTest {

    @Test
    fun formatCombinesFileNameFormattedDateAndDuration() {
        val formatter = RecordingDisplayFormatter(
            dateFormatter = FakeDateFormatter(fixedOutput = "Jan 1, 2024 10:00"),
            durationFormatter = DurationFormatter()
        )
        val recording = RecordingInfo(
            filePath = "/fake/dir/recording_1.m4a",
            fileName = "recording_1.m4a",
            lastModifiedMillis = 123L,
            durationMillis = 65_000L
        )
        assertEquals("recording_1.m4a  •  Jan 1, 2024 10:00  •  1:05", formatter.format(recording))
    }
}