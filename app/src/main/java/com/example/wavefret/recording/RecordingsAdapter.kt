package com.example.wavefret.recording

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wavefret.R

/**
 * RecyclerView adapter that displays a list of recordings, delegating each
 * item's text to RecordingDisplayFormatter and each row's own Play/Stop
 * button tap to onPlayStopClicked.
 *
 * @property displayFormatter Builds each item's display string. Type: RecordingDisplayFormatter
 * @property onPlayStopClicked Called with a recording when its Play/Stop button is tapped. Type: (RecordingInfo) -> Unit
 * @property isPlaying Reports whether a given file path is currently playing, to choose the button label. Type: (String) -> Boolean
 */
class RecordingsAdapter(
    private val displayFormatter: RecordingDisplayFormatter,
    private val onPlayStopClicked: (RecordingInfo) -> Unit,
    private val isPlaying: (String) -> Boolean
) : ListAdapter<RecordingInfo, RecordingsAdapter.RecordingViewHolder>(RecordingDiffCallback()) {

    /**
     * @param parent ViewGroup the new view will be attached to. Type: ViewGroup
     * @param viewType Ignored; this adapter has a single view type. Type: Int
     * @return A new RecordingViewHolder wrapping the inflated item layout. Type: RecordingViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val inflatedView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(inflatedView)
    }

    /**
     * @param holder ViewHolder to bind data into. Type: RecordingViewHolder
     * @param position Index of the recording to bind. Type: Int
     * @return Unit
     */
    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val recording = getItem(position)
        holder.bind(recording, displayFormatter, isPlaying(recording.filePath)) {
            onPlayStopClicked(recording)
        }
    }

    /**
     * Refreshes only the rows for the given file paths, instead of redrawing
     * the whole list, since only their Play/Stop label needs to change.
     *
     * @param previousFilePath File that was playing before the change, or null. Type: String?
     * @param currentFilePath File that is playing now, or null. Type: String?
     * @return Unit
     */
    fun notifyPlaybackChanged(previousFilePath: String?, currentFilePath: String?) {
        notifyRowForFilePath(previousFilePath)
        notifyRowForFilePath(currentFilePath)
    }

    /**
     * @param filePath File path to locate in the current list, or null to no-op. Type: String?
     * @return Unit
     */
    private fun notifyRowForFilePath(filePath: String?) {
        if (filePath == null) return
        val index = currentList.indexOfFirst { it.filePath == filePath }
        if (index != -1) notifyItemChanged(index)
    }

    /**
     * Holds the views for a single recording list item.
     *
     * @property rootView Root view of the inflated item layout. Type: View
     */
    class RecordingViewHolder(private val rootView: View) : RecyclerView.ViewHolder(rootView) {

        private val tvRecordingLabel: TextView = rootView.findViewById(R.id.tvRecordingLabel)
        private val btnPlayStop: Button = rootView.findViewById(R.id.btnPlayStop)

        /**
         * @param recording Recording to display in this row. Type: RecordingInfo
         * @param displayFormatter Builds the display string for the recording. Type: RecordingDisplayFormatter
         * @param isCurrentlyPlaying Whether this row's file is currently playing. Type: Boolean
         * @param onPlayStopClicked Called when this row's Play/Stop button is tapped. Type: () -> Unit
         * @return Unit
         */
        fun bind(
            recording: RecordingInfo,
            displayFormatter: RecordingDisplayFormatter,
            isCurrentlyPlaying: Boolean,
            onPlayStopClicked: () -> Unit
        ) {
            tvRecordingLabel.text = displayFormatter.format(recording)
            btnPlayStop.text = rootView.context.getString(
                if (isCurrentlyPlaying) R.string.stop else R.string.play
            )
            btnPlayStop.setOnClickListener { onPlayStopClicked() }
        }
    }

    /**
     * Tells ListAdapter how to detect item identity and content changes
     * between two RecordingInfo lists, so it can compute a minimal update.
     */
    private class RecordingDiffCallback : DiffUtil.ItemCallback<RecordingInfo>() {

        override fun areItemsTheSame(oldItem: RecordingInfo, newItem: RecordingInfo): Boolean {
            return oldItem.filePath == newItem.filePath
        }

        override fun areContentsTheSame(oldItem: RecordingInfo, newItem: RecordingInfo): Boolean {
            return oldItem == newItem
        }
    }
}