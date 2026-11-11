package com.example.wavefret.practice

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.wavefret.AppContainer
import com.example.wavefret.R
import com.example.wavefret.common.audio.AudioRecordPcmSource
import com.example.wavefret.common.audio.LowPassFilter
import com.example.wavefret.common.audio.LowPassFilteredPitchDetector
import com.example.wavefret.common.audio.Metronome
import com.example.wavefret.common.audio.SilenceGate
import com.example.wavefret.common.audio.ToneGeneratorClickPlayer
import com.example.wavefret.common.audio.YinPitchDetector
import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.time.ElapsedRealtimeClock
import com.example.wavefret.practice.songimport.AsciiTabParser
import com.example.wavefret.practice.songimport.FretboardPositionFinder
import com.example.wavefret.practice.songimport.ImportedExercisesRepository
import com.example.wavefret.practice.songimport.MusicXmlBassParser
import com.example.wavefret.practice.songimport.MusicXmlFileReader
import com.example.wavefret.practice.songimport.SongImporter
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Practice screen: pick a built-in or imported exercise and a mode, then
 * play along. Tabs pasted from the internet and MusicXML files can be
 * imported here too.
 * In wait mode the tab only advances when the right note is heard; in
 * tempo mode a metronome counts in and the tab scrolls at the exercise's
 * tempo, scoring each note as hit or missed.
 *
 * Android framework glue — every decision lives in the tested sessions,
 * [PracticeController], [PracticeDisplayFormatter], [TabStaffLayout] and
 * [Metronome] — so it is verified manually on-device.
 */
class PracticeActivity : AppCompatActivity() {

    private lateinit var exerciseSpinner: Spinner
    private lateinit var practiceModeToggleGroup: MaterialButtonToggleGroup
    private lateinit var tabStaffView: TabStaffView
    private lateinit var currentNoteTextView: TextView
    private lateinit var noteProgressTextView: TextView
    private lateinit var practiceFeedbackTextView: TextView
    private lateinit var startPracticeButton: Button
    private lateinit var importSongButton: Button

    private val appContainer = AppContainer(this)
    private val monotonicClock = ElapsedRealtimeClock()
    private val tabTextParser = TabTextParser(BassTunings.STANDARD)
    private val builtInExercises = BuiltInExercises(tabTextParser)
    private val importedExercisesRepository by lazy {
        ImportedExercisesRepository(appContainer.appStorageDirectoryProvider, TabTextWriter(), tabTextParser)
    }
    private val songImporter = SongImporter(AsciiTabParser(BassTunings.STANDARD), tabTextParser)
    private val musicXmlFileReader = MusicXmlFileReader()
    private val musicXmlBassParser = MusicXmlBassParser(BassTunings.STANDARD, FretboardPositionFinder(BassTunings.STANDARD))
    private var exercises: List<Exercise> = emptyList()
    private val practiceDisplayFormatter = PracticeDisplayFormatter()
    private val practiceController = PracticeController(
        audioSource = AudioRecordPcmSource(samplesPerBufferCount = PRACTICE_SAMPLES_PER_BUFFER_COUNT),
        silenceGate = SilenceGate(),
        pitchDetector = LowPassFilteredPitchDetector(
            lowPassFilter = LowPassFilter(cutoffHz = PITCH_ANALYSIS_CUTOFF_HZ),
            innerPitchDetector = YinPitchDetector()
        ),
        clock = monotonicClock
    )
    private var metronomeClickPlayer: ToneGeneratorClickPlayer? = null

    private var practiceSession: PracticeSession? = null
    private var practiceProgressJob: Job? = null
    private var metronomeJob: Job? = null

    private val requestMicrophonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) startPractice() }

    private val openMusicXmlFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { musicXmlFileUri -> musicXmlFileUri?.let(::importMusicXmlFile) }

    /**
     * @param savedInstanceState Previously saved state, or null on first creation. Type: Bundle?
     * @return Unit
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_practice)
        exerciseSpinner = findViewById(R.id.practiceExerciseSpinner)
        practiceModeToggleGroup = findViewById(R.id.practiceModeToggleGroup)
        tabStaffView = findViewById(R.id.practiceTabStaffView)
        currentNoteTextView = findViewById(R.id.practiceCurrentNoteTextView)
        noteProgressTextView = findViewById(R.id.practiceNoteProgressTextView)
        practiceFeedbackTextView = findViewById(R.id.practiceFeedbackTextView)
        startPracticeButton = findViewById(R.id.practiceStartButton)
        importSongButton = findViewById(R.id.practiceImportSongButton)

        refreshExerciseList(selectedTitle = null)
        practiceModeToggleGroup.check(R.id.practiceWaitModeButton)
        startPracticeButton.setOnClickListener { onStartPracticeClicked() }
        importSongButton.setOnClickListener { showImportSongDialog() }
    }

    /**
     * Reloads built-in and imported exercises into the picker.
     *
     * @param selectedTitle Title to select afterwards, or null to keep the first. Type: String?
     * @return Unit
     */
    private fun refreshExerciseList(selectedTitle: String?) {
        exercises = builtInExercises.all() + importedExercisesRepository.listExercises()
        exerciseSpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, exercises.map { it.title }
        )
        val selectedIndex = exercises.indexOfLast { it.title == selectedTitle }
        if (selectedIndex >= 0) exerciseSpinner.setSelection(selectedIndex)
    }

    /**
     * Shows a dialog where a tab can be pasted, then imports and stores it.
     *
     * @return Unit
     */
    private fun showImportSongDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_import_song, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.importSongTitleEditText)
        val tempoEditText = dialogView.findViewById<EditText>(R.id.importSongTempoEditText)
        val tabEditText = dialogView.findViewById<EditText>(R.id.importSongTabEditText)
        AlertDialog.Builder(this)
            .setTitle(R.string.import_song_dialog_title)
            .setView(dialogView)
            .setPositiveButton(R.string.import_song_confirm) { _, _ ->
                importSong(
                    pastedText = tabEditText.text.toString(),
                    title = titleEditText.text.toString(),
                    tempoBpm = tempoEditText.text.toString().toIntOrNull()?.takeIf { it > 0 }
                )
            }
            .setNeutralButton(R.string.import_song_open_music_xml) { _, _ ->
                openMusicXmlFileLauncher.launch(MUSIC_XML_MIME_TYPES)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Reads a chosen MusicXML (.musicxml, .xml or .mxl) file and stores its bass line.
     *
     * @param musicXmlFileUri Document the player picked. Type: Uri
     * @return Unit
     */
    private fun importMusicXmlFile(musicXmlFileUri: Uri) {
        try {
            val fileBytes = contentResolver.openInputStream(musicXmlFileUri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException(getString(R.string.import_song_unreadable_file))
            val importedExercise = musicXmlBassParser.parse(musicXmlFileReader.readMusicXmlText(fileBytes))
            saveImportedExercise(importedExercise)
        } catch (unreadableScore: IllegalArgumentException) {
            showImportFailure(unreadableScore)
        } catch (unreadableFile: IOException) {
            showImportFailure(unreadableFile)
        }
    }

    /**
     * @param importedExercise Exercise to store and select. Type: Exercise
     * @return Unit
     */
    private fun saveImportedExercise(importedExercise: Exercise) {
        importedExercisesRepository.save(importedExercise)
        refreshExerciseList(selectedTitle = importedExercise.title)
        Toast.makeText(this, getString(R.string.import_song_saved, importedExercise.title), Toast.LENGTH_SHORT).show()
    }

    /**
     * @param importFailure Why the import failed. Type: Exception
     * @return Unit
     */
    private fun showImportFailure(importFailure: Exception) {
        Toast.makeText(this, getString(R.string.import_song_failed, importFailure.message), Toast.LENGTH_LONG).show()
    }

    /**
     * @param pastedText Tab text the player pasted. Type: String
     * @param title Title typed by the player, possibly blank. Type: String
     * @param tempoBpm Tempo typed by the player, or null. Type: Int?
     * @return Unit
     */
    private fun importSong(pastedText: String, title: String, tempoBpm: Int?) {
        try {
            saveImportedExercise(songImporter.import(pastedText, title, tempoBpm))
        } catch (unreadableTab: IllegalArgumentException) {
            showImportFailure(unreadableTab)
        }
    }

    /**
     * Stops listening and the metronome when the screen is no longer visible.
     *
     * @return Unit
     */
    override fun onPause() {
        super.onPause()
        stopPractice()
        metronomeClickPlayer?.release()
        metronomeClickPlayer = null
    }

    /**
     * @return Unit
     */
    private fun onStartPracticeClicked() {
        if (appContainer.microphonePermissionManager.needsPermissionRequest()) {
            requestMicrophonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            startPractice()
        }
    }

    /**
     * Builds a fresh session for the selected exercise and mode and starts
     * listening; in tempo mode it also starts the count-in metronome.
     *
     * @return Unit
     */
    private fun startPractice() {
        stopPractice()
        val selectedExercise = exercises[exerciseSpinner.selectedItemPosition]
        val isTempoMode = practiceModeToggleGroup.checkedButtonId == R.id.practiceTempoModeButton
        val newSession = if (isTempoMode) startTempoSession(selectedExercise) else buildWaitSession(selectedExercise)
        practiceSession = newSession
        startPracticeButton.setText(R.string.practice_restart)
        renderProgress(newSession.exercise, newSession.currentProgress())

        practiceProgressJob = practiceController.progressStream(newSession)
            .transformWhile { progress ->
                emit(progress)
                !progress.isComplete
            }
            .onEach { progress -> renderProgress(newSession.exercise, progress) }
            .launchIn(lifecycleScope)
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
    }

    /**
     * @param exercise Exercise being practiced. Type: Exercise
     * @param progress Latest progress through it. Type: PracticeProgress
     * @return Unit
     */
    private fun renderProgress(exercise: Exercise, progress: PracticeProgress) {
        val displayState = practiceDisplayFormatter.format(exercise, progress)
        tabStaffView.render(exercise, displayState)
        noteProgressTextView.text = getString(
            R.string.practice_note_progress, displayState.currentNoteNumber, displayState.noteCount
        )
        currentNoteTextView.text = if (displayState.currentStringName != null && displayState.currentFret != null) {
            getString(R.string.practice_current_note, displayState.currentStringName, displayState.currentFret)
        } else {
            getString(R.string.practice_score, displayState.scorePercent ?: 0)
        }
        practiceFeedbackTextView.text = getString(feedbackTextFor(displayState.feedback))
        practiceFeedbackTextView.setTextColor(ContextCompat.getColor(this, feedbackColorFor(displayState.feedback)))
        if (displayState.feedback == PracticeFeedback.COMPLETE) stopPractice()
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
     * @param practiceFeedback Feedback to color. Type: PracticeFeedback
     * @return Color resource for it. Type: Int
     */
    private fun feedbackColorFor(practiceFeedback: PracticeFeedback): Int = when (practiceFeedback) {
        PracticeFeedback.CORRECT, PracticeFeedback.COMPLETE -> R.color.tuning_status_in_tune
        PracticeFeedback.WRONG_NOTE -> R.color.tuning_status_out_of_tune
        PracticeFeedback.LISTENING -> R.color.tab_staff_string_line
    }

    private companion object {
        /** ~93 ms at 44.1 kHz: long enough for low E, short enough for tempo mode. */
        const val PRACTICE_SAMPLES_PER_BUFFER_COUNT = 4096
        const val PITCH_ANALYSIS_CUTOFF_HZ = 1000.0
        const val TEMPO_MODE_REQUIRED_MATCHES = 1
        val MUSIC_XML_MIME_TYPES = arrayOf(
            "application/vnd.recordare.musicxml+xml",
            "application/vnd.recordare.musicxml",
            "application/xml",
            "text/xml",
            "application/octet-stream"
        )
    }
}
