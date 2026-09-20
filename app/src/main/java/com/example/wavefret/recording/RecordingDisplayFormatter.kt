package com.example.wavefret.recording

import com.example.wavefret.common.time.DateFormatter
import com.example.wavefret.common.time.DurationFormatter

/**
 * Builds the display string shown for a single recording in the list.
 *
 * @property dateFormatter Formats the recording's timestamp for display. Type: DateFormatter
 * @property durationFormatter Formats the recording's duration for display. Type: DurationFormatter
 */
class RecordingDisplayFormatter(
    private val dateFormatter: DateFormatter,
    private val durationFormatter: DurationFormatter
) {

    /**
     * @param recording Recording to describe. Type: RecordingInfo
     * @return Display string combining file name, formatted date, and duration. Type: String
     */
    fun format(recording: RecordingInfo): String {
        val formattedDate = dateFormatter.format(recording.lastModifiedMillis)
        val formattedDuration = durationFormatter.format(recording.durationMillis)
        return "${recording.fileName}  •  $formattedDate  •  $formattedDuration"
    }
}