package com.example.wavefret.practice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wavefret.AppContainer
import com.example.wavefret.R
import com.example.wavefret.common.audio.LowPassFilter
import com.example.wavefret.common.audio.MediaCodecAudioDecoder
import com.example.wavefret.common.audio.Metronome
import com.example.wavefret.common.audio.ToneGeneratorClickPlayer
import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.time.ElapsedRealtimeClock
import com.example.wavefret.common.ui.WaveFretActivity
import com.example.wavefret.common.ui.applySystemBarPadding
import com.example.wavefret.common.ui.showMicrophonePermissionNeeded
import com.example.wavefret.practice.playalong.LessonAudioStore
import com.example.wavefret.practice.playalong.ListenActivity
import com.example.wavefret.practice.playalong.PlayAlongActivity
import com.example.wavefret.practice.playalong.YouTubeLinkParser
import com.example.wavefret.practice.progression.ExercisePickerItem
import com.example.wavefret.practice.progression.ExercisePickerItemsBuilder
import com.example.wavefret.practice.progression.PerformanceRating
import com.example.wavefret.practice.progression.PracticeMode
import com.example.wavefret.practice.progression.PracticeResult
import com.example.wavefret.practice.progression.PracticeSummary
import com.example.wavefret.practice.progression.PracticeSummaryBuilder
import com.example.wavefret.practice.songimport.FretboardPositionFinder
import com.example.wavefret.practice.songimport.LessonSynchronizer
import com.example.wavefret.practice.songimport.OnDeviceLessonTranscriber
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Practice screen. The chosen exercise sits in a card that opens a picker
 * sheet (easiest first, with level, best score, mastery and the
 * recommendation); a mode toggle explains wait vs tempo mode; the tab is
 * previewed before starting; Start/Stop sits in the thumb zone; and a
 * finished run ends on a summary sheet with the score, badges and a
 * one-tap "next exercise". The app bar opens the song library to add songs.
 *
 * Android framework glue — every decision lives in the tested sessions,
 * [PracticeController], [PracticeDisplayFormatter], [TabStaffLayout],
 * [Metronome], importers and progression classes — so it is verified on-device.
 */
class PracticeActivity : WaveFretActivity() {

    private lateinit var rootView: ViewGroup
    private lateinit var exerciseCard: View
    private lateinit var exerciseTitleTextView: TextView
    private lateinit var exerciseDetailTextView: TextView
    private lateinit var practiceModeToggleGroup: MaterialButtonToggleGroup
    private lateinit var modeHelpTextView: TextView
    private lateinit var currentNoteTextView: TextView
    private lateinit var feedbackTextView: TextView
    private lateinit var tabStaffView: TabStaffView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var noteProgressTextView: TextView
    private lateinit var startStopButton: MaterialButton

    private val appContainer = AppContainer(this)
    private val monotonicClock = ElapsedRealtimeClock()
    private val practiceDisplayFormatter = PracticeDisplayFormatter()
    private val exercisePickerItemsBuilder by lazy { ExercisePickerItemsBuilder(appContainer.exerciseProgression) }
    private val practiceSummaryBuilder by lazy { PracticeSummaryBuilder(appContainer.exerciseProgression) }
    private val practiceController = PracticeListeningPipeline.createController(monotonicClock)
    private var metronomeClickPlayer: ToneGeneratorClickPlayer? = null

    private var exercisePickerItems: List<ExercisePickerItem> = emptyList()
    private var selectedExercise: Exercise? = null

    private val syncAudioPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { audioUri ->
        audioUri?.let(::syncLessonWithAudio)
    }
    private var practiceProgressJob: Job? = null
    private var metronomeJob: Job? = null

    private val requestMicrophonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startPractice() else showMicrophonePermissionNeeded(rootView, R.string.practice_permission_needed)
    }

    /**
     * @param savedInstanceState Previously saved state, or null on first creation. Type: Bundle?
     * @return Unit
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_practice)
        rootView = findViewById(R.id.practiceRoot)
        exerciseCard = findViewById(R.id.practiceExerciseCard)
        exerciseTitleTextView = findViewById(R.id.practiceExerciseTitleTextView)
        exerciseDetailTextView = findViewById(R.id.practiceExerciseDetailTextView)
        practiceModeToggleGroup = findViewById(R.id.practiceModeToggleGroup)
        modeHelpTextView = findViewById(R.id.practiceModeHelpTextView)
        currentNoteTextView = findViewById(R.id.practiceCurrentNoteTextView)
        feedbackTextView = findViewById(R.id.practiceFeedbackTextView)
        tabStaffView = findViewById(R.id.practiceTabStaffView)
        progressIndicator = findViewById(R.id.practiceProgressIndicator)
        noteProgressTextView = findViewById(R.id.practiceNoteProgressTextView)
        startStopButton = findViewById(R.id.practiceStartButton)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setTitle(R.string.practice_title)
        topAppBar.setNavigationOnClickListener { finish() }
        topAppBar.inflateMenu(R.menu.menu_practice)
        topAppBar.setOnMenuItemClickListener { menuItem ->
            val exercise = selectedExercise
            when (menuItem.itemId) {
                R.id.practiceImportSongMenuItem -> startActivity(SongLibraryActivity.intentFor(this, showAddSong = true))
                R.id.practiceEditLessonMenuItem -> exercise?.let {
                    startActivity(LessonEditorActivity.intentFor(this, it, originalTitle = it.title))
                }
                R.id.practiceAddYouTubeMenuItem -> exercise?.let(::askForYouTubeVideoToSync)
                R.id.practiceAddAudioMenuItem -> exercise?.let { syncAudioPicker.launch(arrayOf("audio/*", "video/mp4")) }
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        topAppBar.applySystemBarPadding(applyTop = true, applyBottom = false)
        findViewById<View>(R.id.practiceBottomBar).applySystemBarPadding(applyTop = false, applyBottom = true)

        practiceModeToggleGroup.check(R.id.practiceWaitModeButton)
        practiceModeToggleGroup.addOnButtonCheckedListener { _, _, isChecked -> if (isChecked) onPracticeModeChanged() }
        exerciseCard.setOnClickListener { showExercisePicker() }
        findViewById<View>(R.id.practicePlayAlongButton).setOnClickListener {
            selectedExercise?.let { exercise -> startActivity(PlayAlongActivity.intentFor(this, exercise.title)) }
        }
        startStopButton.setOnClickListener { onStartStopClicked() }

        refreshExercises(preferredTitle = intent.getStringExtra(EXTRA_EXERCISE_TITLE))
    }

    /**
     * Stops listening and the metronome when the screen is no longer visible.
     *
     * @return Unit
     */
    override fun onPause() {
        super.onPause()
        stopPractice()
        selectedExercise?.let(::showPreview)
        metronomeClickPlayer?.release()
        metronomeClickPlayer = null
    }

    /**
     * Reloads exercises and history, then selects the preferred title, the
     * current one, or the recommended one, in that order.
     *
     * @param preferredTitle Title to select if present, or null. Type: String?
     * @return Unit
     */
    private fun refreshExercises(preferredTitle: String?) {
        val results = appContainer.practiceResultsRepository.listResults()
        val importedTitles = appContainer.importedExercisesRepository.listExercises().map { it.title }.toSet()
        exercisePickerItems = exercisePickerItemsBuilder.build(appContainer.allExercises(), results, importedTitles)
        val chosenItem = exercisePickerItems.lastOrNull { it.exercise.title == preferredTitle }
            ?: exercisePickerItems.lastOrNull { it.exercise.title == selectedExercise?.title }
            ?: exercisePickerItems.firstOrNull { it.isRecommended }
            ?: exercisePickerItems.firstOrNull()
        chosenItem?.let(::selectExercise)
    }

    /**
     * @param pickerItem Exercise the player chose. Type: ExercisePickerItem
     * @return Unit
     */
    private fun selectExercise(pickerItem: ExercisePickerItem) {
        stopPractice()
        selectedExercise = pickerItem.exercise
        exerciseTitleTextView.text = pickerItem.exercise.title
        exerciseDetailTextView.text = describeExercise(pickerItem)
        exerciseCard.contentDescription = getString(R.string.practice_choose_exercise_description, pickerItem.exercise.title)
        tabStaffView.contentDescription = getString(R.string.practice_tab_description, pickerItem.exercise.title)
        findViewById<View>(R.id.practicePlayAlongButton).isVisible =
            pickerItem.exercise.noteOnsetsMillis != null && pickerItem.exercise.mediaSource != null
        val practiceMenu = findViewById<MaterialToolbar>(R.id.topAppBar).menu
        listOf(R.id.practiceEditLessonMenuItem, R.id.practiceAddYouTubeMenuItem, R.id.practiceAddAudioMenuItem).forEach { itemId ->
            practiceMenu.findItem(itemId)?.isVisible = pickerItem.isImported
        }
        showPreview(pickerItem.exercise)
    }

    /**
     * Asks for a YouTube link, then opens Listen mode to give the lesson
     * that video's timing for play-along.
     *
     * @param lesson Lesson to add the video to. Type: Exercise
     * @return Unit
     */
    private fun askForYouTubeVideoToSync(lesson: Exercise) {
        val linkLayout = layoutInflater.inflate(R.layout.dialog_youtube_link, rootView, false) as TextInputLayout
        val linkEditText = requireNotNull(linkLayout.editText)
        val linkDialog = MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_play)
            .setTitle(R.string.practice_add_youtube)
            .setMessage(R.string.practice_add_youtube_message)
            .setView(linkLayout)
            .setPositiveButton(R.string.next, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        linkDialog.setOnShowListener {
            linkDialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val videoId = YouTubeLinkParser().videoIdFrom(linkEditText.text.toString())
                if (videoId == null) {
                    linkLayout.error = getString(R.string.youtube_link_invalid)
                } else {
                    linkDialog.dismiss()
                    startActivity(ListenActivity.intentFor(this, videoId, syncLessonTitle = lesson.title))
                }
            }
        }
        linkDialog.show()
    }

    /**
     * Gives the selected lesson the timing of an audio file: the file is
     * decoded and transcribed, the lesson's notes are lined up with what was
     * heard, and the file is kept for play-along.
     *
     * @param audioUri Audio file chosen by the player. Type: Uri
     * @return Unit
     */
    private fun syncLessonWithAudio(audioUri: Uri) {
        val lesson = selectedExercise ?: return
        val progressView = layoutInflater.inflate(R.layout.dialog_transcribing, rootView, false)
        progressView.findViewById<TextView>(R.id.transcribingStepTextView).setText(R.string.practice_syncing_audio)
        val progressDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.practice_add_audio)
            .setView(progressView)
            .setCancelable(false)
            .create()
        val syncJob = lifecycleScope.launch {
            try {
                val syncedLesson = withContext(Dispatchers.Default) {
                    val decodedAudio = MediaCodecAudioDecoder(this@PracticeActivity).decode(audioUri)
                    val heardNotes = OnDeviceLessonTranscriber(this@PracticeActivity)
                        .transcribeNotes(decodedAudio.samples, decodedAudio.sampleRateHz, isFullSong = true)
                    val storedAudioName = contentResolver.openInputStream(audioUri)?.use { audioStream ->
                        LessonAudioStore(appContainer.appStorageDirectoryProvider).store(lesson.title + AUDIO_FILE_SUFFIX, audioStream)
                    } ?: throw IllegalArgumentException(getString(R.string.import_song_unreadable_file))
                    LessonSynchronizer(FretboardPositionFinder(lesson.tuning))
                        .synchronize(lesson, heardNotes, LessonMediaSource.StoredAudio(storedAudioName))
                }
                appContainer.importedExercisesRepository.save(syncedLesson)
                progressDialog.dismiss()
                refreshExercises(preferredTitle = syncedLesson.title)
                Snackbar.make(rootView, R.string.practice_song_added, Snackbar.LENGTH_LONG)
                    .setAnchorView(startStopButton)
                    .setAction(R.string.practice_play_along) { startActivity(PlayAlongActivity.intentFor(this@PracticeActivity, syncedLesson.title)) }
                    .show()
            } catch (syncFailure: IllegalArgumentException) {
                progressDialog.dismiss()
                Snackbar.make(rootView, getString(R.string.transcription_failed, syncFailure.message), Snackbar.LENGTH_LONG)
                    .setAnchorView(startStopButton)
                    .show()
            } catch (deniedFile: SecurityException) {
                progressDialog.dismiss()
                Snackbar.make(rootView, R.string.import_song_unreadable_file, Snackbar.LENGTH_LONG).setAnchorView(startStopButton).show()
            }
        }
        progressDialog.setButton(android.content.DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel)) { _, _ -> syncJob.cancel() }
        progressDialog.show()
    }

    /**
     * @param pickerItem Exercise to describe. Type: ExercisePickerItem
     * @return Secondary line such as "Beginner · 60 BPM · 8 notes · Best 85%". Type: String
     */
    private fun describeExercise(pickerItem: ExercisePickerItem): String {
        val detailParts = mutableListOf(
            getString(labelResourceFor(pickerItem.difficultyLevel)),
            getString(R.string.practice_exercise_tempo, pickerItem.exercise.tempoBpm),
            resources.getQuantityString(
                R.plurals.practice_exercise_notes, pickerItem.exercise.noteCount, pickerItem.exercise.noteCount
            )
        )
        pickerItem.bestScorePercent?.let { detailParts += getString(R.string.practice_exercise_best, it) }
        return detailParts.joinToString(DETAIL_SEPARATOR)
    }

    /**
     * Shows the exercise's first note and the tab before anything is played,
     * so the player can read ahead instead of facing an empty screen.
     *
     * @param exercise Exercise to preview. Type: Exercise
     * @return Unit
     */
    private fun showPreview(exercise: Exercise) {
        renderProgress(exercise, buildWaitSession(exercise).currentProgress())
        feedbackTextView.setText(R.string.practice_feedback_ready)
        feedbackTextView.setTextColor(colorFor(com.google.android.material.R.attr.colorOnSurfaceVariant))
    }

    /**
     * @return Unit
     */
    private fun onPracticeModeChanged() {
        modeHelpTextView.setText(
            if (isTempoModeSelected()) R.string.practice_mode_tempo_help else R.string.practice_mode_wait_help
        )
        selectedExercise?.let { exercise ->
            stopPractice()
            showPreview(exercise)
        }
    }

    /**
     * @return True if tempo mode is selected. Type: Boolean
     */
    private fun isTempoModeSelected(): Boolean = practiceModeToggleGroup.checkedButtonId == R.id.practiceTempoModeButton

    /**
     * @return Unit
     */
    private fun showExercisePicker() {
        val pickerSheet = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_exercise_picker, rootView, false)
        sheetView.findViewById<RecyclerView>(R.id.exercisePickerRecyclerView).apply {
            layoutManager = LinearLayoutManager(this@PracticeActivity)
            adapter = ExercisePickerAdapter(
                pickerItems = exercisePickerItems,
                describeExercise = ::describeExercise,
                onExerciseChosen = { pickerItem ->
                    selectExercise(pickerItem)
                    pickerSheet.dismiss()
                },
                onDeleteClicked = { pickerItem ->
                    pickerSheet.dismiss()
                    confirmDeleteSong(pickerItem.exercise)
                }
            )
        }
        pickerSheet.setContentView(sheetView)
        pickerSheet.show()
    }

    /**
     * Asks before removing an imported song; its practice history is kept.
     *
     * @param song Imported song to delete. Type: Exercise
     * @return Unit
     */
    private fun confirmDeleteSong(song: Exercise) {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_delete)
            .setTitle(R.string.delete_song_title)
            .setMessage(getString(R.string.delete_song_message, song.title))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                appContainer.importedExercisesRepository.delete(song.title)
                (song.mediaSource as? LessonMediaSource.StoredAudio)?.let { storedAudio ->
                    LessonAudioStore(appContainer.appStorageDirectoryProvider).delete(storedAudio.fileName)
                }
                if (selectedExercise?.title == song.title) selectedExercise = null
                refreshExercises(preferredTitle = null)
            }
            .show()
    }

    /**
     * Start when idle, stop (back to the preview) while running.
     *
     * @return Unit
     */
    private fun onStartStopClicked() {
        when {
            practiceProgressJob != null -> selectedExercise?.let { exercise ->
                stopPractice()
                showPreview(exercise)
            }
            appContainer.microphonePermissionManager.needsPermissionRequest() ->
                requestMicrophonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            else -> startPractice()
        }
    }

    /**
     * Builds a fresh session for the selected exercise and mode and starts
     * listening; in tempo mode it also starts the count-in metronome.
     *
     * @return Unit
     */
    private fun startPractice() {
        val exercise = selectedExercise ?: return
        stopPractice()
        val practiceMode = if (isTempoModeSelected()) PracticeMode.TEMPO else PracticeMode.WAIT
        val practiceSession = if (practiceMode == PracticeMode.TEMPO) startTempoSession(exercise) else buildWaitSession(exercise)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        startStopButton.setText(R.string.practice_stop)
        startStopButton.setIconResource(R.drawable.ic_stop)

        practiceProgressJob = practiceController.progressStream(practiceSession)
            .transformWhile { progress ->
                emit(progress)
                !progress.isComplete
            }
            .onEach { progress ->
                renderProgress(exercise, progress)
                if (progress.isComplete) onPracticeCompleted(exercise, practiceMode, progress)
            }
            .launchIn(lifecycleScope)
        renderProgress(exercise, practiceSession.currentProgress())
    }

    /**
     * @param exercise Exercise to practice. Type: Exercise
     * @return A wait-mode session for it. Type: PracticeSession
     */
    private fun buildWaitSession(exercise: Exercise): PracticeSession {
        tabStaffView.playheadPositionProvider = null
        return WaitModePracticeSession(exercise, NoteMatcher(), HitConfirmer())
    }

    /**
     * Starts the clock, metronome and smooth scrolling for tempo mode.
     *
     * @param exercise Exercise to practice. Type: Exercise
     * @return A started tempo-mode session for it. Type: PracticeSession
     */
    private fun startTempoSession(exercise: Exercise): PracticeSession {
        val tempoSession = TempoModePracticeSession(
            exercise = exercise,
            noteMatcher = NoteMatcher(),
            hitConfirmer = HitConfirmer(requiredConsecutiveMatches = TEMPO_MODE_REQUIRED_MATCHES)
        )
        val beatClock = tempoSession.start(monotonicClock.currentTimeMillis())
        val clickPlayer = metronomeClickPlayer ?: ToneGeneratorClickPlayer().also { metronomeClickPlayer = it }
        val metronome = Metronome(clickPlayer, monotonicClock)
        metronomeJob = lifecycleScope.launch {
            metronome.play(beatClock, beatCount = tempoSession.countInBeats + exercise.noteCount)
        }
        tabStaffView.playheadPositionProvider = {
            beatClock.beatPositionAt(monotonicClock.currentTimeMillis()) - tempoSession.countInBeats
        }
        return tempoSession
    }

    /**
     * @return Unit
     */
    private fun stopPractice() {
        practiceProgressJob?.cancel()
        practiceProgressJob = null
        metronomeJob?.cancel()
        metronomeJob = null
        tabStaffView.playheadPositionProvider = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        startStopButton.setText(R.string.practice_start)
        startStopButton.setIconResource(R.drawable.ic_play)
    }

    /**
     * @param exercise Exercise being practiced. Type: Exercise
     * @param progress Latest progress through it. Type: PracticeProgress
     * @return Unit
     */
    private fun renderProgress(exercise: Exercise, progress: PracticeProgress) {
        val displayState = practiceDisplayFormatter.format(exercise, progress)
        tabStaffView.render(exercise, displayState)
        noteProgressTextView.text = getString(R.string.practice_note_progress, displayState.currentNoteNumber, displayState.noteCount)
        progressIndicator.max = displayState.noteCount
        progressIndicator.setProgressCompat(progress.hitCount + progress.missCount, true)

        val currentStringName = displayState.currentStringName
        val currentFret = displayState.currentFret
        if (currentStringName != null && currentFret != null) {
            val isOpenString = currentFret == 0
            currentNoteTextView.text = if (isOpenString) {
                getString(R.string.practice_current_note_open, currentStringName)
            } else {
                getString(R.string.practice_current_note, currentStringName, currentFret)
            }
            currentNoteTextView.contentDescription = if (isOpenString) {
                getString(R.string.practice_current_note_open_description, currentStringName)
            } else {
                getString(R.string.practice_current_note_description, currentStringName, currentFret)
            }
        } else {
            currentNoteTextView.text = getString(R.string.summary_score, displayState.scorePercent ?: 0)
            currentNoteTextView.contentDescription = null
        }

        val isCountingIn = practiceProgressJob != null && progress.playheadPosition < 0 && !progress.isComplete
        feedbackTextView.setText(if (isCountingIn) R.string.practice_feedback_count_in else feedbackTextFor(displayState.feedback))
        feedbackTextView.setTextColor(colorFor(feedbackColorAttributeFor(displayState.feedback)))
    }

    /**
     * Stores the run and shows the summary sheet.
     *
     * @param exercise Exercise that was completed. Type: Exercise
     * @param practiceMode Mode it was played in. Type: PracticeMode
     * @param progress Final progress. Type: PracticeProgress
     * @return Unit
     */
    private fun onPracticeCompleted(exercise: Exercise, practiceMode: PracticeMode, progress: PracticeProgress) {
        stopPractice()
        val earlierResults = appContainer.practiceResultsRepository.listResults()
        val finishedResult = PracticeResult(
            exerciseTitle = exercise.title,
            practiceMode = practiceMode,
            scorePercent = practiceDisplayFormatter.format(exercise, progress).scorePercent ?: 0,
            hitCount = progress.hitCount,
            missCount = progress.missCount,
            completedAtMillis = appContainer.clock.currentTimeMillis()
        )
        appContainer.practiceResultsRepository.record(finishedResult)
        val practiceSummary = practiceSummaryBuilder.build(finishedResult, earlierResults, appContainer.allExercises())
        refreshExercises(preferredTitle = exercise.title)
        showSummary(practiceSummary)
    }

    /**
     * @param practiceSummary What to celebrate and suggest. Type: PracticeSummary
     * @return Unit
     */
    private fun showSummary(practiceSummary: PracticeSummary) {
        val summarySheet = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_practice_summary, rootView, false)
        sheetView.findViewById<TextView>(R.id.practiceSummaryRatingTextView).setText(ratingTextFor(practiceSummary.rating))
        sheetView.findViewById<TextView>(R.id.practiceSummaryScoreTextView).text =
            getString(R.string.summary_score, practiceSummary.result.scorePercent)
        sheetView.findViewById<TextView>(R.id.practiceSummaryHitsTextView).text =
            getString(R.string.summary_hits_misses, practiceSummary.result.hitCount, practiceSummary.result.missCount)
        sheetView.findViewById<View>(R.id.practiceSummaryNewBestChip).isVisible = practiceSummary.isNewBest
        sheetView.findViewById<View>(R.id.practiceSummaryMasteredChip).isVisible = practiceSummary.isJustMastered

        val nextButton = sheetView.findViewById<MaterialButton>(R.id.practiceSummaryNextButton)
        val nextExercise = practiceSummary.nextExercise
        if (nextExercise != null) {
            nextButton.text = getString(R.string.summary_next, nextExercise.title)
            nextButton.setOnClickListener {
                summarySheet.dismiss()
                refreshExercises(preferredTitle = nextExercise.title)
            }
        } else {
            nextButton.setText(R.string.summary_done)
            nextButton.setOnClickListener { summarySheet.dismiss() }
        }
        sheetView.findViewById<MaterialButton>(R.id.practiceSummaryTryAgainButton).setOnClickListener {
            summarySheet.dismiss()
            startPractice()
        }
        summarySheet.setContentView(sheetView)
        summarySheet.show()
    }

    /**
     * @param practiceFeedback Feedback to describe. Type: PracticeFeedback
     * @return String resource describing it. Type: Int
     */
    private fun feedbackTextFor(practiceFeedback: PracticeFeedback): Int = when (practiceFeedback) {
        PracticeFeedback.LISTENING -> R.string.practice_feedback_listening
        PracticeFeedback.CORRECT -> R.string.practice_feedback_correct
        PracticeFeedback.WRONG_NOTE -> R.string.practice_feedback_wrong_note
        PracticeFeedback.COMPLETE -> R.string.practice_feedback_complete
    }

    /**
     * @param practiceFeedback Feedback to colour. Type: PracticeFeedback
     * @return Theme colour role for it. Type: Int
     */
    private fun feedbackColorAttributeFor(practiceFeedback: PracticeFeedback): Int = when (practiceFeedback) {
        PracticeFeedback.CORRECT, PracticeFeedback.COMPLETE -> com.google.android.material.R.attr.colorSecondary
        PracticeFeedback.WRONG_NOTE -> androidx.appcompat.R.attr.colorError
        PracticeFeedback.LISTENING -> com.google.android.material.R.attr.colorOnSurfaceVariant
    }

    /**
     * @param performanceRating Verdict on a run. Type: PerformanceRating
     * @return String resource for its headline. Type: Int
     */
    private fun ratingTextFor(performanceRating: PerformanceRating): Int = when (performanceRating) {
        PerformanceRating.PERFECT -> R.string.summary_perfect
        PerformanceRating.GREAT -> R.string.summary_great
        PerformanceRating.GOOD -> R.string.summary_good
        PerformanceRating.KEEP_PRACTICING -> R.string.summary_keep_practicing
    }

    /**
     * @param colorAttribute Theme colour role. Type: Int
     * @return The resolved colour. Type: Int
     */
    private fun colorFor(colorAttribute: Int): Int = MaterialColors.getColor(rootView, colorAttribute)

    companion object {
        private const val EXTRA_EXERCISE_TITLE = "com.example.wavefret.practice.EXERCISE_TITLE"
        private const val DETAIL_SEPARATOR = " · "

        private const val TEMPO_MODE_REQUIRED_MATCHES = 1
        private const val AUDIO_FILE_SUFFIX = ".audio"

        /**
         * @param context Context starting the screen. Type: Context
         * @param exerciseTitle Exercise to open with, or null for the recommended one. Type: String?
         * @return An intent that opens the practice screen. Type: Intent
         */
        fun intentFor(context: Context, exerciseTitle: String?): Intent {
            return Intent(context, PracticeActivity::class.java).putExtra(EXTRA_EXERCISE_TITLE, exerciseTitle)
        }
    }
}
