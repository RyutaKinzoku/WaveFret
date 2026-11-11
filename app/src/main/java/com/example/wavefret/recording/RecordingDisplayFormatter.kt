package com.example.wavefret.recording

import com.example.wavefret.common.time.DateFormatter
import com.example.wavefret.common.time.DurationFormatter

/**
 * Builds the text shown for a single recording in the list.
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
     * @return Its title: when it was recorded, which means more to a player than a file name. Type: String
     */
    fun formatTitle(recording: RecordingInfo): String = dateFormatter.format(recording.lastModifiedMillis)

    /**
     * @param recording Recording to describe. Type: RecordingInfo
     * @return Its length as minutes and seconds. Type: String
     */
    fun formatDuration(recording: RecordingInfo): String = durationFormatter.format(recording.durationMillis)

    /**
     * @param recording Recording to describe. Type: RecordingInfo
     * @return One phrase for screen readers combining title and length. Type: String
     */
    fun formatAccessibilityDescription(recording: RecordingInfo): String {
        return "${formatTitle(recording)}, ${formatDuration(recording)}"
    }
}
