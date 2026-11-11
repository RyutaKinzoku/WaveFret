package com.example.wavefret.recording

import android.Manifest
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wavefret.AppContainer
import com.example.wavefret.R
import com.example.wavefret.common.time.ElapsedRealtimeClock
import com.example.wavefret.common.ui.applySystemBarPadding
import com.example.wavefret.common.ui.showMicrophonePermissionNeeded
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Recorder: one large record/stop button in the thumb zone, a running
 * timer while recording, and the list of takes with play and delete.
 * Deleting asks for confirmation, and leaving the screen mid-take saves it
 * instead of losing it.
 *
 * Android framework glue — decisions live in the tested
 * [RecordingUiController], [RecordingSessionController], [PlaybackController],
 * [RecordingsRepository] and [RecordingDisplayFormatter] — so it is verified on-device.
 */
class RecorderActivity : AppCompatActivity() {

    private val appContainer = AppContainer(this)
    private val elapsedRealtimeClock = ElapsedRealtimeClock()
    private val recordingUiController = RecordingUiController()
    private val recordingDirectoryProvider by lazy { RecordingsFolderProvider(appContainer.appStorageDirectoryProvider) }
    private val recordingSessionController by lazy {
        RecordingSessionController(
            audioRecorder = MediaRecorderAudioRecorder(this),
            fileNamer = RecordingFileNamer(appContainer.clock),
            directoryProvider = recordingDirectoryProvider
        )
    }
    private val recordingsRepository by lazy {
        RecordingsRepository(recordingDirectoryProvider, MediaMetadataRetrieverDurationReader())
    }
    private val recordingDisplayFormatter by lazy {
        RecordingDisplayFormatter(appContainer.dateFormatter, appContainer.durationFormatter)
    }
    private val playbackController: PlaybackController = PlaybackController(
        audioPlayer = MediaPlayerAudioPlayer(),
        onPlaybackStateChanged = { previousFilePath, currentFilePath ->
            recordingsAdapter.notifyPlaybackChanged(previousFilePath, currentFilePath)
        }
    )
    private val recordingsAdapter: RecordingsAdapter by lazy {
        RecordingsAdapter(
            displayFormatter = recordingDisplayFormatter,
            onPlayStopClicked = { recording -> playbackController.onItemClicked(recording.filePath) },
            onDeleteClicked = ::confirmDeleteRecording,
            isPlaying = playbackController::isPlaying
        )
    }

    private lateinit var rootView: View
    private lateinit var timerTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var takesRecyclerView: RecyclerView
    private lateinit var emptyStateView: View
    private lateinit var toggleButton: FloatingActionButton

    private var timerJob: Job? = null

    private val requestMicrophonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startRecording() else showMicrophonePermissionNeeded(rootView, R.string.recorder_permission_needed)
    }

    /**
     * @param savedInstanceState Previously saved state, or null on first creation. Type: Bundle?
     * @return Unit
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recorder)
        rootView = findViewById(R.id.recorderRoot)
        timerTextView = findViewById(R.id.recorderTimerTextView)
        statusTextView = findViewById(R.id.recorderStatusTextView)
        takesRecyclerView = findViewById(R.id.recorderTakesRecyclerView)
        emptyStateView = findViewById(R.id.recorderEmptyState)
        toggleButton = findViewById(R.id.recorderToggleButton)

        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setTitle(R.string.recorder_title)
        topAppBar.setNavigationOnClickListener { finish() }
        topAppBar.applySystemBarPadding(applyTop = true, applyBottom = false)
        findViewById<View>(R.id.recorderBottomBar).applySystemBarPadding(applyTop = false, applyBottom = true)

        takesRecyclerView.layoutManager = LinearLayoutManager(this)
        takesRecyclerView.adapter = recordingsAdapter
        toggleButton.setOnClickListener { onToggleRecordingClicked() }

        renderRecordingState()
        refreshTakes()
    }

    /**
     * Saves any take in progress and stops playback when leaving the screen.
     *
     * @return Unit
     */
    override fun onPause() {
        super.onPause()
        if (recordingUiController.currentState() == RecordingState.RECORDING) stopRecording()
        playbackController.stopPlayback()
    }

    /**
     * @return Unit
     */
    private fun onToggleRecordingClicked() {
        when (recordingUiController.currentState()) {
            RecordingState.IDLE -> if (appContainer.microphonePermissionManager.needsPermissionRequest()) {
                requestMicrophonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                startRecording()
            }
            RecordingState.RECORDING -> stopRecording()
        }
    }

    /**
     * @return Unit
     */
    private fun startRecording() {
        playbackController.stopPlayback()
        recordingUiController.onRecordClicked()
        recordingSessionController.startNewRecording()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val startedAtMillis = elapsedRealtimeClock.currentTimeMillis()
        timerJob = lifecycleScope.launch {
            while (isActive) {
                timerTextView.text = appContainer.durationFormatter.format(
                    elapsedRealtimeClock.currentTimeMillis() - startedAtMillis
                )
                delay(TIMER_REFRESH_MILLIS)
            }
        }
        renderRecordingState()
    }

    /**
     * @return Unit
     */
    private fun stopRecording() {
        timerJob?.cancel()
        timerJob = null
        recordingUiController.onStopClicked()
        recordingSessionController.stopCurrentRecording()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        renderRecordingState()
        refreshTakes()
        Snackbar.make(rootView, R.string.recording_saved, Snackbar.LENGTH_SHORT).setAnchorView(toggleButton).show()
    }

    /**
     * @return Unit
     */
    private fun renderRecordingState() {
        val isRecording = recordingUiController.currentState() == RecordingState.RECORDING
        statusTextView.setText(if (isRecording) R.string.recorder_status_recording else R.string.recorder_status_idle)
        toggleButton.setImageResource(if (isRecording) R.drawable.ic_stop else R.drawable.ic_mic)
        toggleButton.contentDescription = getString(
            if (isRecording) R.string.recorder_stop_description else R.string.recorder_start_description
        )
        if (!isRecording) timerTextView.setText(R.string.recorder_timer_zero)
    }

    /**
     * @return Unit
     */
    private fun refreshTakes() {
        val takes = recordingsRepository.listRecordings()
        recordingsAdapter.submitList(takes)
        emptyStateView.isVisible = takes.isEmpty()
        findViewById<View>(R.id.recorderTakesHeading).isVisible = takes.isNotEmpty()
    }

    /**
     * Asks before deleting, since a deleted take cannot be recovered.
     *
     * @param recording Take the player wants to delete. Type: RecordingInfo
     * @return Unit
     */
    private fun confirmDeleteRecording(recording: RecordingInfo) {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_delete)
            .setTitle(R.string.delete_recording_title)
            .setMessage(getString(R.string.delete_recording_message, recordingDisplayFormatter.formatTitle(recording)))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                if (playbackController.isPlaying(recording.filePath)) playbackController.stopPlayback()
                recordingsRepository.deleteRecording(recording.filePath)
                refreshTakes()
            }
            .show()
    }

    private companion object {
        const val TIMER_REFRESH_MILLIS = 250L
    }
}
