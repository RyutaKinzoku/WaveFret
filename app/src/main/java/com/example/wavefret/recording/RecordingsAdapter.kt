package com.example.wavefret.recording

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wavefret.R

/**
 * RecyclerView adapter that displays a list of recordings, delegating each
 * item's text to RecordingDisplayFormatter and reporting taps upward via
 * onItemClicked.
 *
 * @property displayFormatter Builds each item's display string. Type: RecordingDisplayFormatter
 * @property onItemClicked Called with a recording when its row is tapped. Type: (RecordingInfo) -> Unit
 * @property isPlaying Reports whether a given file path is currently playing, for highlighting. Type: (String) -> Boolean
 */
class RecordingsAdapter(
    private val displayFormatter: RecordingDisplayFormatter,
    private val onItemClicked: (RecordingInfo) -> Unit,
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
        holder.bind(recording, displayFormatter, isPlaying(recording.filePath))
        holder.itemView.setOnClickListener { onItemClicked(recording) }
    }

    /**
     * Holds the views for a single recording list item.
     *
     * @property rootView Root view of the inflated item layout. Type: View
     */
    class RecordingViewHolder(private val rootView: View) : RecyclerView.ViewHolder(rootView) {

        private val tvRecordingLabel: TextView = rootView.findViewById(R.id.tvRecordingLabel)

        /**
         * @param recording Recording to display in this row. Type: RecordingInfo
         * @param displayFormatter Builds the display string for the recording. Type: RecordingDisplayFormatter
         * @param isCurrentlyPlaying Whether this row's file is currently playing. Type: Boolean
         * @return Unit
         */
        fun bind(recording: RecordingInfo, displayFormatter: RecordingDisplayFormatter, isCurrentlyPlaying: Boolean) {
            val prefix = if (isCurrentlyPlaying) "▶  " else ""
            tvRecordingLabel.text = prefix + displayFormatter.format(recording)
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