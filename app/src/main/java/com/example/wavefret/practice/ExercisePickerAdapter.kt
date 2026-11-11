package com.example.wavefret.practice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.wavefret.R
import com.example.wavefret.practice.progression.ExercisePickerItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors

/**
 * Rows of the exercise picker sheet: title, level, tempo and best score,
 * a check for mastered exercises, a "Recommended" badge, and a delete
 * button for imported songs only. Rows come from the tested
 * [com.example.wavefret.practice.progression.ExercisePickerItemsBuilder].
 *
 * @property pickerItems Rows to show, easiest first. Type: List<ExercisePickerItem>
 * @property describeExercise Builds a row's secondary line. Type: (ExercisePickerItem) -> String
 * @property onExerciseChosen Called when a row is tapped. Type: (ExercisePickerItem) -> Unit
 * @property onDeleteClicked Called when an imported song's delete button is tapped. Type: (ExercisePickerItem) -> Unit
 * @property onEditClicked Called when an imported song's edit button is tapped, or null to hide it. Type: ((ExercisePickerItem) -> Unit)?
 */
class ExercisePickerAdapter(
    private val pickerItems: List<ExercisePickerItem>,
    private val describeExercise: (ExercisePickerItem) -> String,
    private val onExerciseChosen: (ExercisePickerItem) -> Unit,
    private val onDeleteClicked: (ExercisePickerItem) -> Unit,
    private val onEditClicked: ((ExercisePickerItem) -> Unit)? = null
) : RecyclerView.Adapter<ExercisePickerAdapter.ExercisePickerViewHolder>() {

    /**
     * @param parent ViewGroup the row will be attached to. Type: ViewGroup
     * @param viewType Ignored; one view type. Type: Int
     * @return A holder for a new row. Type: ExercisePickerViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExercisePickerViewHolder {
        return ExercisePickerViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_exercise_picker, parent, false)
        )
    }

    /** @return Number of rows. Type: Int */
    override fun getItemCount(): Int = pickerItems.size

    /**
     * @param holder Row to fill. Type: ExercisePickerViewHolder
     * @param position Index of the row. Type: Int
     * @return Unit
     */
    override fun onBindViewHolder(holder: ExercisePickerViewHolder, position: Int) {
        val pickerItem = pickerItems[position]
        holder.bind(
            pickerItem = pickerItem,
            detailText = describeExercise(pickerItem),
            isDeletable = pickerItem.isImported,
            onChosen = { onExerciseChosen(pickerItem) },
            onDelete = { onDeleteClicked(pickerItem) },
            onEdit = onEditClicked?.let { editCallback -> { editCallback(pickerItem) } }
        )
    }

    /**
     * Holds one picker row's views.
     *
     * @param rowView The inflated row layout. Type: View
     */
    class ExercisePickerViewHolder(private val rowView: View) : RecyclerView.ViewHolder(rowView) {

        private val statusImageView: ImageView = rowView.findViewById(R.id.exercisePickerStatusImageView)
        private val titleTextView: TextView = rowView.findViewById(R.id.exercisePickerTitleTextView)
        private val detailTextView: TextView = rowView.findViewById(R.id.exercisePickerDetailTextView)
        private val recommendedTextView: TextView = rowView.findViewById(R.id.exercisePickerRecommendedTextView)
        private val deleteButton: MaterialButton = rowView.findViewById(R.id.exercisePickerDeleteButton)
        private val editButton: MaterialButton = rowView.findViewById(R.id.exercisePickerEditButton)

        /**
         * @param pickerItem Row data. Type: ExercisePickerItem
         * @param detailText Secondary line. Type: String
         * @param isDeletable Whether to show the delete button. Type: Boolean
         * @param onChosen Called when the row is tapped. Type: () -> Unit
         * @param onDelete Called when delete is tapped. Type: () -> Unit
         * @param onEdit Called when edit is tapped, or null to hide the button. Type: (() -> Unit)?
         * @return Unit
         */
        fun bind(
            pickerItem: ExercisePickerItem,
            detailText: String,
            isDeletable: Boolean,
            onChosen: () -> Unit,
            onDelete: () -> Unit,
            onEdit: (() -> Unit)?
        ) {
            val context = rowView.context
            titleTextView.text = pickerItem.exercise.title
            detailTextView.text = detailText
            recommendedTextView.isVisible = pickerItem.isRecommended
            statusImageView.setImageResource(if (pickerItem.isMastered) R.drawable.ic_check_circle else R.drawable.ic_music_note)
            statusImageView.setColorFilter(
                MaterialColors.getColor(
                    rowView,
                    if (pickerItem.isMastered) com.google.android.material.R.attr.colorSecondary
                    else com.google.android.material.R.attr.colorOnSurfaceVariant
                )
            )
            statusImageView.contentDescription =
                if (pickerItem.isMastered) context.getString(R.string.exercise_picker_mastered_description) else null
            statusImageView.importantForAccessibility =
                if (pickerItem.isMastered) View.IMPORTANT_FOR_ACCESSIBILITY_YES else View.IMPORTANT_FOR_ACCESSIBILITY_NO
            deleteButton.isVisible = isDeletable
            deleteButton.contentDescription = context.getString(R.string.exercise_picker_delete_description, pickerItem.exercise.title)
            rowView.setOnClickListener { onChosen() }
            deleteButton.setOnClickListener { onDelete() }
            editButton.isVisible = isDeletable && onEdit != null
            editButton.contentDescription = context.getString(R.string.edit_song_description, pickerItem.exercise.title)
            editButton.setOnClickListener { onEdit?.invoke() }
        }
    }
}
