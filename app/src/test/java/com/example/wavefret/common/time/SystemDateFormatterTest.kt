package com.example.wavefret.common.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class SystemDateFormatterTest {

    @Test
    fun formatRendersMonthDayYearAndTime() {
        val calendar = Calendar.getInstance(Locale.US)
        calendar.set(2024, Calendar.JANUARY, 5, 14, 30, 0)
        val formatter = SystemDateFormatter()
        assertEquals("Jan 5, 2024 14:30", formatter.format(calendar.timeInMillis))
    }
}