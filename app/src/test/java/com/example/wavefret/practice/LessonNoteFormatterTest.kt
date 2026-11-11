package com.example.wavefret.practice

import org.junit.Assert.assertEquals
import org.junit.Test

class LessonNoteFormatterTest {

    private val formatter = LessonNoteFormatter()

    @Test
    fun timeShowsMinutesSecondsAndTenths() {
        assertEquals("0:03.4", formatter.formatTime(3_449))
        assertEquals("1:05.0", formatter.formatTime(65_000))
    }

    @Test
    fun noTimingShowsADash() {
        assertEquals("–", formatter.formatTime(null))
    }

    @Test
    fun noteNumbersStartAtOne() {
        assertEquals("1", formatter.formatNumber(0))
    }
}
