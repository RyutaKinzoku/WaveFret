package com.example.wavefret.practice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wavefret.AppContainer
import com.example.wavefret.R
import com.example.wavefret.common.ui.WaveFretActivity
import com.example.wavefret.common.ui.applySystemBarPadding
import com.example.wavefret.practice.playalong.PlayAlongActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale

/**
 * Lets the player check and fix a lesson, typically one transcribed from
 * audio: rename it, set its tempo, preview it as tab, and tap any note to
 * change its string or fret, delete it, or add a note after it. Leaving
 * with unsaved edits asks first.
 *
 * Android framework glue — every edit goes through the tested
 * [LessonEditor] and rows through [LessonNoteFormatter] — verified on-device.
 */
class LessonEditorActivity : WaveFretActivity() {

    private lateinit var rootView: ViewGroup
    private lateinit var notesRecyclerView: RecyclerView
    private lateinit var headerView: View
    private lateinit var tabStaffView: TabStaffView
    private lateinit var titleEditText: TextInputEditText
    private lateinit var tempoEditText: TextInputEditText

    private val appContainer = AppContainer(this)
    private val lessonNoteFormatter = LessonNoteFormatter()
    private val practiceDisplayFormatter = PracticeDisplayFormatter()
    private lateinit var lessonEditor: LessonEditor
    private var originalTitle: String? = null
    private var selectedNoteIndex = 0
    private var hasUnsavedChanges = false

    /**
     * @param savedInstanceState Previously saved state, or null on first creation. Type: Bundle?
     * @return Unit
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lesson_editor)
        rootView = findViewById(R.id.lessonEditorRoot)
        notesRecyclerView = findViewById(R.id.lessonEditorNotesRecyclerView)

        val draftTabText = savedInstanceState?.getString(STATE_DRAFT_TAB_TEXT) ?: intent.getStringExtra(EXTRA_DRAFT_TAB_TEXT)
        lessonEditor = LessonEditor(appContainer.tabTextParser.parse(requireNotNull(draftTabText)))
        originalTitle = intent.getStringExtra(EXTRA_ORIGINAL_TITLE)
        hasUnsavedChanges = savedInstanceState?.getBoolean(STATE_HAS_UNSAVED_CHANGES) ?: (originalTitle == null)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setTitle(R.string.lesson_editor_title)
        topAppBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        topAppBar.applySystemBarPadding(applyTop = true, applyBottom = false)
        findViewById<View>(R.id.lessonEditorBottomBar).applySystemBarPadding(applyTop = false, applyBottom = true)
        findViewById<MaterialButton>(R.id.lessonEditorSaveButton).setOnClickListener { saveLesson() }

        // The layout manager must exist before inflating a child against the list.
        notesRecyclerView.layoutManager = LinearLayoutManager(this)
        headerView = LayoutInflater.from(this).inflate(R.layout.header_lesson_editor, notesRecyclerView, false)
        tabStaffView = headerView.findViewById(R.id.lessonEditorTabStaffView)
        titleEditText = headerView.findViewById(R.id.lessonEditorTitleEditText)
        tempoEditText = headerView.findViewById(R.id.lessonEditorTempoEditText)
        titleEditText.setText(lessonEditor.lesson.title)
        // Plain ASCII digits, so the value parses back with toIntOrNull() in every locale.
        tempoEditText.setText(String.format(Locale.ROOT, "%d", lessonEditor.lesson.tempoBpm))
        titleEditText.addTextChangedListener(markChangedWatcher())
        tempoEditText.addTextChangedListener(markChangedWatcher())

        notesRecyclerView.adapter = LessonNotesAdapter()
        renderLesson()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = confirmLeaving()
        })
    }

    /**
     * Keeps edits across rotation and process death.
     *
     * @param outState Bundle to save into. Type: Bundle
     * @return Unit
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_DRAFT_TAB_TEXT, TabTextWriter().write(lessonEditor.lesson))
        outState.putBoolean(STATE_HAS_UNSAVED_CHANGES, hasUnsavedChanges)
    }

    /**
     * @return Unit
     */
    private fun renderLesson() {
        val lesson = lessonEditor.lesson
        selectedNoteIndex = selectedNoteIndex.coerceIn(0, lesson.noteCount - 1)
        val previewProgress = PracticeProgress(
            currentNoteIndex = selectedNoteIndex,
            noteCount = lesson.noteCount,
            hitCount = 0,
            missCount = 0,
            lastReading = PitchReading.SILENT
        )
        val previewState = practiceDisplayFormatter.format(lesson, previewProgress).copy(
            noteStatuses = List(lesson.noteCount) { index ->
                if (index == selectedNoteIndex) TabNoteStatus.CURRENT else TabNoteStatus.UPCOMING
            }
        )
        tabStaffView.render(lesson, previewState)
        headerView.findViewById<TextView>(R.id.lessonEditorNotesHeading).text =
            getString(R.string.lesson_editor_notes_heading, lesson.noteCount)
    }

    /**
     * @param noteIndex Note the player tapped. Type: Int
     * @return Unit
     */
    private fun showNoteEditor(noteIndex: Int) {
        val previousSelectedIndex = selectedNoteIndex
        selectedNoteIndex = noteIndex
        renderLesson()
        notesRecyclerView.adapter?.notifyItemChanged(previousSelectedIndex + 1)
        notesRecyclerView.adapter?.notifyItemChanged(noteIndex + 1)
        val lesson = lessonEditor.lesson
        val tabNote = lesson.notes[noteIndex]
        val noteSheet = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_edit_note, rootView, false)
        sheetView.findViewById<TextView>(R.id.editNoteTitleTextView).text =
            getString(R.string.edit_note_title, lessonNoteFormatter.formatNumber(noteIndex))

        val stringChipGroup = sheetView.findViewById<ChipGroup>(R.id.editNoteStringChipGroup)
        lesson.tuning.strings.forEachIndexed { stringIndex, bassString ->
            val stringChip = layoutInflater.inflate(R.layout.item_string_chip, stringChipGroup, false) as Chip
            stringChip.id = View.generateViewId()
            stringChip.text = bassString.name
            stringChip.tag = stringIndex
            stringChip.isChecked = stringIndex == tabNote.stringIndex
            stringChipGroup.addView(stringChip)
        }
        val fretLabel = sheetView.findViewById<TextView>(R.id.editNoteFretLabel)
        val fretSlider = sheetView.findViewById<Slider>(R.id.editNoteFretSlider)
        fretSlider.value = tabNote.fret.toFloat().coerceIn(fretSlider.valueFrom, fretSlider.valueTo)
        fretLabel.text = getString(R.string.edit_note_fret_label, tabNote.fret)

        /** Applies the sheet's current string and fret to the note. */
        fun applyPosition() {
            val checkedChip = stringChipGroup.findViewById<Chip>(stringChipGroup.checkedChipId) ?: return
            val fret = fretSlider.value.toInt()
            fretLabel.text = getString(R.string.edit_note_fret_label, fret)
            applyEdit(lessonEditor.changeNote(noteIndex, TabNote(checkedChip.tag as Int, fret)))
            notesRecyclerView.adapter?.notifyItemChanged(noteIndex + 1)
        }
        stringChipGroup.setOnCheckedStateChangeListener { _, _ -> applyPosition() }
        fretSlider.addOnChangeListener { _, _, isFromUser -> if (isFromUser) applyPosition() }

        val deleteButton = sheetView.findViewById<MaterialButton>(R.id.editNoteDeleteButton)
        deleteButton.isEnabled = lesson.noteCount > 1
        deleteButton.setOnClickListener {
            noteSheet.dismiss()
            applyEdit(lessonEditor.deleteNote(noteIndex))
            notesRecyclerView.adapter?.notifyItemRemoved(noteIndex + 1)
            notesRecyclerView.adapter?.notifyItemRangeChanged(0, lessonEditor.lesson.noteCount + 1)
        }
        sheetView.findViewById<MaterialButton>(R.id.editNoteInsertButton).setOnClickListener {
            noteSheet.dismiss()
            applyEdit(lessonEditor.insertNoteAfter(noteIndex, lessonEditor.lesson.notes[noteIndex]))
            notesRecyclerView.adapter?.notifyItemInserted(noteIndex + 2)
            notesRecyclerView.adapter?.notifyItemRangeChanged(0, lessonEditor.lesson.noteCount + 1)
            showNoteEditor(noteIndex + 1)
        }
        noteSheet.setContentView(sheetView)
        noteSheet.show()
    }

    /**
     * @param editedLesson Editor holding the result of an edit. Type: LessonEditor
     * @return Unit
     */
    private fun applyEdit(editedLesson: LessonEditor) {
        lessonEditor = editedLesson
        hasUnsavedChanges = true
        renderLesson()
    }

    /**
     * Validates title and tempo, stores the lesson and opens it: in
     * play-along when it has a song attached (step 2 after Listen mode),
     * otherwise in practice.
     *
     * @return Unit
     */
    private fun saveLesson() {
        val titleLayout = headerView.findViewById<TextInputLayout>(R.id.lessonEditorTitleLayout)
        val tempoLayout = headerView.findViewById<TextInputLayout>(R.id.lessonEditorTempoLayout)
        val title = titleEditText.text?.toString().orEmpty()
        val tempoBpm = tempoEditText.text?.toString()?.toIntOrNull()
        titleLayout.error = if (title.isBlank()) getString(R.string.lesson_editor_title_required) else null
        tempoLayout.error = if (tempoBpm == null || tempoBpm !in TEMPO_RANGE_BPM) getString(R.string.lesson_editor_tempo_invalid) else null
        if (title.isBlank() || tempoBpm == null || tempoBpm !in TEMPO_RANGE_BPM) {
            notesRecyclerView.scrollToPosition(0)
            return
        }
        val savedLesson = lessonEditor.rename(title).changeTempo(tempoBpm).lesson
        appContainer.importedExercisesRepository.save(savedLesson)
        originalTitle?.takeIf { it != savedLesson.title }?.let(appContainer.importedExercisesRepository::delete)
        hasUnsavedChanges = false
        Toast.makeText(this, getString(R.string.lesson_editor_saved, savedLesson.title), Toast.LENGTH_SHORT).show()
        val canPlayAlong = savedLesson.mediaSource != null && savedLesson.noteOnsetsMillis != null
        startActivity(
            if (canPlayAlong) PlayAlongActivity.intentFor(this, savedLesson.title)
            else PracticeActivity.intentFor(this, savedLesson.title)
        )
        finish()
    }

    /**
     * @return Unit
     */
    private fun confirmLeaving() {
        if (!hasUnsavedChanges) {
            finish()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.lesson_editor_discard_title)
            .setMessage(R.string.lesson_editor_discard_message)
            .setNegativeButton(R.string.lesson_editor_keep_editing, null)
            .setPositiveButton(R.string.lesson_editor_discard) { _, _ -> finish() }
            .show()
    }

    /** @return A watcher that marks the lesson as edited. Type: android.text.TextWatcher */
    private fun markChangedWatcher() = object : android.text.TextWatcher {
        override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
            hasUnsavedChanges = true
        }
        override fun afterTextChanged(editable: android.text.Editable?) = Unit
    }

    /** Header (details and preview) followed by one row per note. */
    private inner class LessonNotesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        /**
         * @param position Row position. Type: Int
         * @return Header or note row type. Type: Int
         */
        override fun getItemViewType(position: Int): Int = if (position == 0) HEADER_VIEW_TYPE else NOTE_VIEW_TYPE

        /** @return Header plus one row per note. Type: Int */
        override fun getItemCount(): Int = lessonEditor.lesson.noteCount + 1

        /**
         * @param parent List the row goes in. Type: ViewGroup
         * @param viewType Header or note row. Type: Int
         * @return A holder for the row. Type: RecyclerView.ViewHolder
         */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val rowView = if (viewType == HEADER_VIEW_TYPE) {
                (headerView.parent as? ViewGroup)?.removeView(headerView)
                headerView
            } else {
                LayoutInflater.from(parent.context).inflate(R.layout.item_lesson_note, parent, false)
            }
            return object : RecyclerView.ViewHolder(rowView) {}
        }

        /**
         * @param holder Row to fill. Type: RecyclerView.ViewHolder
         * @param position Row position. Type: Int
         * @return Unit
         */
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (position == 0) return
            val noteIndex = position - 1
            val lesson = lessonEditor.lesson
            val tabNote = lesson.notes[noteIndex]
            val number = lessonNoteFormatter.formatNumber(noteIndex)
            val time = lessonNoteFormatter.formatTime(lesson.noteOnsetsMillis?.get(noteIndex))
            val stringName = tabNote.stringName(lesson.tuning)
            val notePosition = if (tabNote.fret == 0) {
                getString(R.string.practice_current_note_open, stringName)
            } else {
                getString(R.string.practice_current_note, stringName, tabNote.fret)
            }
            holder.itemView.findViewById<TextView>(R.id.lessonNoteNumberTextView).text = number
            holder.itemView.findViewById<TextView>(R.id.lessonNoteTimeTextView).text = time
            holder.itemView.findViewById<TextView>(R.id.lessonNotePositionTextView).text = notePosition
            holder.itemView.contentDescription = getString(R.string.lesson_editor_note_description, number, time, notePosition)
            holder.itemView.isSelected = noteIndex == selectedNoteIndex
            holder.itemView.setOnClickListener { showNoteEditor(noteIndex) }
        }
    }

    companion object {
        private const val EXTRA_DRAFT_TAB_TEXT = "com.example.wavefret.practice.DRAFT_TAB_TEXT"
        private const val EXTRA_ORIGINAL_TITLE = "com.example.wavefret.practice.ORIGINAL_TITLE"
        private const val STATE_DRAFT_TAB_TEXT = "draftTabText"
        private const val STATE_HAS_UNSAVED_CHANGES = "hasUnsavedChanges"
        private const val HEADER_VIEW_TYPE = 0
        private const val NOTE_VIEW_TYPE = 1
        private val TEMPO_RANGE_BPM = 20..300

        /**
         * @param context Context starting the editor. Type: Context
         * @param lesson Lesson to edit. Type: Exercise
         * @param originalTitle Title it is stored under, or null for a new draft. Type: String?
         * @return An intent that opens the editor. Type: Intent
         */
        fun intentFor(context: Context, lesson: Exercise, originalTitle: String?): Intent {
            return Intent(context, LessonEditorActivity::class.java)
                .putExtra(EXTRA_DRAFT_TAB_TEXT, TabTextWriter().write(lesson))
                .putExtra(EXTRA_ORIGINAL_TITLE, originalTitle)
        }
    }
}
