package com.example.wavefret.common.time

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatterTest {

    private val formatter = DurationFormatter()

    @Test
    fun formatZeroMillisReturnsZeroZero() {
        assertEquals("0:00", formatter.format(0L))
    }

    @Test
    fun formatFiveSecondsPadsSecondsWithLeadingZero() {
        assertEquals("0:05", formatter.format(5_000L))
    }

    @Test
    fun formatSixtySecondsRollsOverToOneMinute() {
        assertEquals("1:00", formatter.format(60_000L))
    }

    @Test
    fun formatSixtyFiveSecondsShowsOneMinuteFiveSeconds() {
        assertEquals("1:05", formatter.format(65_000L))
    }

    @Test
    fun formatTruncatesPartialSecondsInsteadOfRounding() {
        assertEquals("0:09", formatter.format(9_999L))
    }
}