package com.example.wavefret.recording

import com.example.wavefret.common.time.DurationFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingDisplayFormatterTest {

    private val formatter = RecordingDisplayFormatter(
        dateFormatter = FakeDateFormatter(fixedOutput = "Jan 1, 2024 10:00"),
        durationFormatter = DurationFormatter()
    )
    private val recording = RecordingInfo(
        filePath = "/fake/dir/recording_1.m4a",
        fileName = "recording_1.m4a",
        lastModifiedMillis = 123L,
        durationMillis = 65_000L
    )

    @Test
    fun titleIsTheRecordingDateRatherThanTheFileName() {
        assertEquals("Jan 1, 2024 10:00", formatter.formatTitle(recording))
    }

    @Test
    fun durationIsFormattedAsMinutesAndSeconds() {
        assertEquals("1:05", formatter.formatDuration(recording))
    }

    @Test
    fun accessibilityDescriptionCombinesDateAndDuration() {
        assertEquals("Jan 1, 2024 10:00, 1:05", formatter.formatAccessibilityDescription(recording))
    }
}
