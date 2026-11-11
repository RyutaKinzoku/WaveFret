package com.example.wavefret.recording

import com.example.wavefret.common.time.Clock

/**
 * Generates unique file names for new recordings, based on the current time.
 *
 * @property clock Source of the current time, injected for testability. Type: Clock
 */
class RecordingFileNamer(private val clock: Clock) {

    /**
     * @param mediaType Whether the take is audio or video. Type: RecordingMediaType
     * @return A timestamped file name, e.g. "recording_1700000000000.m4a". Type: String
     */
    fun generateFileName(mediaType: RecordingMediaType = RecordingMediaType.AUDIO): String {
        return "recording_${clock.currentTimeMillis()}.${mediaType.fileExtension}"
    }
}