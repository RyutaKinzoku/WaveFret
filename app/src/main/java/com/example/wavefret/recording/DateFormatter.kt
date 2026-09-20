package com.example.wavefret.recording

/**
 * Abstraction over formatting a timestamp into a human-readable string, so
 * display logic can be unit tested deterministically without depending on
 * the JVM's default locale or time zone.
 */
interface DateFormatter {
    /**
     * @param epochMillis Timestamp to format, in epoch milliseconds. Type: Long
     * @return Human-readable representation of the timestamp. Type: String
     */
    fun format(epochMillis: Long): String
}