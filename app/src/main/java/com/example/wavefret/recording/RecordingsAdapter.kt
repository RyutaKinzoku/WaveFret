package com.example.wavefret.recording

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.wavefret.R

/**
 * RecyclerView adapter that displays a list of recordings, delegating each
 * item's text to RecordingDisplayFormatter.
 *
 * @property displayFormatter Builds each item's display string. Type: RecordingDisplayFormatter
 */
class RecordingsAdapter(
    private val displayFormatter: RecordingDisplayFormatter
) : RecyclerView.Adapter<RecordingsAdapter.RecordingViewHolder>() {

    private var recordings: List<RecordingInfo> = emptyList()

    /**
     * Replaces the displayed list of recordings and refreshes the view.
     *
     * @param newRecordings Updated list of recordings to display. Type: List<RecordingInfo>
     * @return Unit
     */
    fun submitList(newRecordings: List<RecordingInfo>) {
        recordings = newRecordings
        notifyDataSetChanged()
    }

    /**
     * @param parent ViewGroup the new view will be attached to. Type: ViewGroup
     * @param viewType Ignored; this adapter has a single view type. Type: Int
     * @return A new RecordingViewHolder wrapping the inflated item layout. Type: RecordingViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return RecordingViewHolder(view)
    }

    /**
     * @param holder ViewHolder to bind data into. Type: RecordingViewHolder
     * @param position Index of the recording to bind. Type: Int
     * @return Unit
     */
    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        holder.bind(recordings[position], displayFormatter)
    }

    /** @return Number of recordings currently displayed. Type: Int */
    override fun getItemCount(): Int = recordings.size

    /**
     * Holds the views for a single recording list item.
     *
     * @property itemView Root view of the inflated item layout. Type: View
     */
    class RecordingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvRecordingLabel: TextView = itemView.findViewById(R.id.tvRecordingLabel)

        /**
         * @param recording Recording to display in this row. Type: RecordingInfo
         * @param displayFormatter Builds the display string for the recording. Type: RecordingDisplayFormatter
         * @return Unit
         */
        fun bind(recording: RecordingInfo, displayFormatter: RecordingDisplayFormatter) {
            tvRecordingLabel.text = displayFormatter.format(recording)
        }
    }
}