package com.example.wavefret.recording

/**
 * Generates unique file names for new recordings, based on the current time.
 *
 * @property clock Source of the current time, injected for testability. Type: Clock
 */
class RecordingFileNamer(private val clock: Clock) {

    /**
     * @return A timestamped .m4a file name, e.g. "recording_1700000000000.m4a". Type: String
     */
    fun generateFileName(): String {
        return "recording_${clock.currentTimeMillis()}.m4a"
    }
}