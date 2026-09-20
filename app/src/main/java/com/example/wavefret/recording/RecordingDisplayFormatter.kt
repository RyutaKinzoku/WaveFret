package com.example.wavefret.recording

import com.example.wavefret.common.time.DateFormatter

/**
 * Builds the display string shown for a single recording in the list.
 *
 * @property dateFormatter Formats the recording's timestamp for display. Type: DateFormatter
 */
class RecordingDisplayFormatter(private val dateFormatter: DateFormatter) {

    /**
     * @param recording Recording to describe. Type: RecordingInfo
     * @return Display string combining file name and formatted date. Type: String
     */
    fun format(recording: RecordingInfo): String {
        return "${recording.fileName}  •  ${dateFormatter.format(recording.lastModifiedMillis)}"
    }
}