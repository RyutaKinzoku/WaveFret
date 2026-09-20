package com.example.wavefret.common.time

import java.util.Locale

/**
 * Formats a duration in milliseconds as a minutes:seconds string (e.g. "1:05").
 * Pure formatting logic with no external dependencies, so it's tested
 * directly with a real instance rather than through a fake.
 */
class DurationFormatter {

    /**
     * @param durationMillis Duration to format, in milliseconds. Type: Long
     * @return Duration formatted as "m:ss", truncated to whole seconds. Type: String
     */
    fun format(durationMillis: Long): String {
        val totalSeconds = durationMillis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}