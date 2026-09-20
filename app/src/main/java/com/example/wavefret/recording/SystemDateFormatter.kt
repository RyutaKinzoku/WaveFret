package com.example.wavefret.recording

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Real DateFormatter backed by SimpleDateFormat, using the device's default locale. */
class SystemDateFormatter : DateFormatter {

    private val pattern = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())

    /**
     * @param epochMillis Timestamp to format, in epoch milliseconds. Type: Long
     * @return Formatted date/time string, e.g. "Jan 1, 2024 10:00". Type: String
     */
    override fun format(epochMillis: Long): String = pattern.format(Date(epochMillis))
}