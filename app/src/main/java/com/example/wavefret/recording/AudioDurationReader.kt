package com.example.wavefret.recording

/**
 * Abstraction over reading an audio file's duration, so any logic that
 * needs a recording's length can be unit tested without depending on the
 * Android media framework.
 */
interface AudioDurationReader {
    /**
     * @param filePath Absolute path of the audio file to inspect. Type: String
     * @return Duration of the file in milliseconds, or 0 if it could not be read. Type: Long
     */
    fun readDurationMillis(filePath: String): Long
}