package com.example.wavefret.recording

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wavefret.R
import com.google.android.material.button.MaterialButton

/**
 * RecyclerView adapter showing each take as a card with a play/stop icon
 * button, its date and length, and a delete icon button. Taps are passed
 * upward; text comes from the tested [RecordingDisplayFormatter].
 *
 * @property displayFormatter Builds each take's title, length and spoken description. Type: RecordingDisplayFormatter
 * @property onPlayStopClicked Called with a take when its play/stop button is tapped. Type: (RecordingInfo) -> Unit
 * @property onDeleteClicked Called with a take when its delete button is tapped. Type: (RecordingInfo) -> Unit
 * @property isPlaying Reports whether a given file path is currently playing. Type: (String) -> Boolean
 */
class RecordingsAdapter(
    private val displayFormatter: RecordingDisplayFormatter,
    private val onPlayStopClicked: (RecordingInfo) -> Unit,
    private val onDeleteClicked: (RecordingInfo) -> Unit,
    private val isPlaying: (String) -> Boolean
) : ListAdapter<RecordingInfo, RecordingsAdapter.RecordingViewHolder>(RecordingDiffCallback()) {

    /**
     * @param parent ViewGroup the new view will be attached to. Type: ViewGroup
     * @param viewType Ignored; this adapter has a single view type. Type: Int
     * @return A new RecordingViewHolder wrapping the inflated item layout. Type: RecordingViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val inflatedView = LayoutInflater.from(parent.context).inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(inflatedView)
    }

    /**
     * @param holder ViewHolder to bind data into. Type: RecordingViewHolder
     * @param position Index of the take to bind. Type: Int
     * @return Unit
     */
    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val recording = getItem(position)
        holder.bind(
            recording = recording,
            displayFormatter = displayFormatter,
            isCurrentlyPlaying = isPlaying(recording.filePath),
            onPlayStopClicked = { onPlayStopClicked(recording) },
            onDeleteClicked = { onDeleteClicked(recording) }
        )
    }

    /**
     * Refreshes only the rows whose play state changed.
     *
     * @param previousFilePath File that was playing before the change, or null. Type: String?
     * @param currentFilePath File that is playing now, or null. Type: String?
     * @return Unit
     */
    fun notifyPlaybackChanged(previousFilePath: String?, currentFilePath: String?) {
        notifyRowForFilePath(previousFilePath)
        if (currentFilePath != previousFilePath) notifyRowForFilePath(currentFilePath)
    }

    /**
     * @param filePath File whose row should be redrawn, or null for none. Type: String?
     * @return Unit
     */
    private fun notifyRowForFilePath(filePath: String?) {
        if (filePath == null) return
        val rowIndex = currentList.indexOfFirst { it.filePath == filePath }
        if (rowIndex >= 0) notifyItemChanged(rowIndex)
    }

    /**
     * Holds one take card's views.
     *
     * @param rootView The inflated item layout. Type: View
     */
    class RecordingViewHolder(rootView: View) : RecyclerView.ViewHolder(rootView) {

        private val titleTextView: TextView = rootView.findViewById(R.id.recordingTitleTextView)
        private val durationTextView: TextView = rootView.findViewById(R.id.recordingDurationTextView)
        private val playStopButton: MaterialButton = rootView.findViewById(R.id.recordingPlayStopButton)
        private val deleteButton: MaterialButton = rootView.findViewById(R.id.recordingDeleteButton)
        private val textContainer: View = rootView.findViewById(R.id.recordingTextContainer)

        /**
         * @param recording Take to display. Type: RecordingInfo
         * @param displayFormatter Builds the take's text. Type: RecordingDisplayFormatter
         * @param isCurrentlyPlaying Whether this take is playing. Type: Boolean
         * @param onPlayStopClicked Called when play/stop is tapped. Type: () -> Unit
         * @param onDeleteClicked Called when delete is tapped. Type: () -> Unit
         * @return Unit
         */
        fun bind(
            recording: RecordingInfo,
            displayFormatter: RecordingDisplayFormatter,
            isCurrentlyPlaying: Boolean,
            onPlayStopClicked: () -> Unit,
            onDeleteClicked: () -> Unit
        ) {
            val context = itemView.context
            val title = displayFormatter.formatTitle(recording)
            titleTextView.text = title
            durationTextView.text = displayFormatter.formatDuration(recording)
            textContainer.contentDescription = displayFormatter.formatAccessibilityDescription(recording)
            playStopButton.setIconResource(if (isCurrentlyPlaying) R.drawable.ic_stop else R.drawable.ic_play)
            playStopButton.contentDescription = context.getString(
                if (isCurrentlyPlaying) R.string.recording_stop_description else R.string.recording_play_description, title
            )
            deleteButton.contentDescription = context.getString(R.string.recording_delete_description, title)
            playStopButton.setOnClickListener { onPlayStopClicked() }
            deleteButton.setOnClickListener { onDeleteClicked() }
        }
    }

    /** Detects item identity and content changes between two take lists. */
    private class RecordingDiffCallback : DiffUtil.ItemCallback<RecordingInfo>() {

        /**
         * @param oldItem Take in the old list. Type: RecordingInfo
         * @param newItem Take in the new list. Type: RecordingInfo
         * @return True if both refer to the same file. Type: Boolean
         */
        override fun areItemsTheSame(oldItem: RecordingInfo, newItem: RecordingInfo): Boolean {
            return oldItem.filePath == newItem.filePath
        }

        /**
         * @param oldItem Take in the old list. Type: RecordingInfo
         * @param newItem Take in the new list. Type: RecordingInfo
         * @return True if nothing displayed has changed. Type: Boolean
         */
        override fun areContentsTheSame(oldItem: RecordingInfo, newItem: RecordingInfo): Boolean {
            return oldItem == newItem
        }
    }
}
