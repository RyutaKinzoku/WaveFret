package com.example.wavefret

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wavefret.common.permission.SystemPermissionChecker
import com.example.wavefret.common.storage.ExternalAppStorageDirectoryProvider
import com.example.wavefret.common.time.DurationFormatter
import com.example.wavefret.common.time.SystemClock
import com.example.wavefret.common.time.SystemDateFormatter
import com.example.wavefret.recording.AudioPermissionManager
import com.example.wavefret.recording.MediaMetadataRetrieverDurationReader
import com.example.wavefret.recording.MediaPlayerAudioPlayer
import com.example.wavefret.recording.MediaRecorderAudioRecorder
import com.example.wavefret.recording.PlaybackController
import com.example.wavefret.recording.RecordingDisplayFormatter
import com.example.wavefret.recording.RecordingFileNamer
import com.example.wavefret.recording.RecordingInfo
import com.example.wavefret.recording.RecordingSessionController
import com.example.wavefret.recording.RecordingState
import com.example.wavefret.recording.RecordingUiController
import com.example.wavefret.recording.RecordingsAdapter
import com.example.wavefret.recording.RecordingsFolderProvider
import com.example.wavefret.recording.RecordingsRepository

/**
 * App entry point. Sets up edge-to-edge layout, requests microphone access,
 * wires the record/stop toggle button to RecordingUiController and
 * RecordingSessionController, and displays, plays back, and deletes past
 * recordings.
 */
class MainActivity : AppCompatActivity() {

    /** Decides whether RECORD_AUDIO needs to be requested. Type: AudioPermissionManager */
    private val audioPermissionManager = AudioPermissionManager(SystemPermissionChecker(this))

    /** Holds recording UI state (button label, status text), independent of Android Views. Type: RecordingUiController */
    private val recordingUiController = RecordingUiController()

    /** Resolves the app's base storage directory. Type: ExternalAppStorageDirectoryProvider */
    private val appStorageDirectoryProvider = ExternalAppStorageDirectoryProvider(this)

    /** Resolves where recording files are stored, as a "recordings" subfolder. Type: RecordingsFolderProvider */
    private val recordingDirectoryProvider = RecordingsFolderProvider(appStorageDirectoryProvider)

    /** Drives the actual MediaRecorder lifecycle for each recording session. Type: RecordingSessionController */
    private val recordingSessionController = RecordingSessionController(
        audioRecorder = MediaRecorderAudioRecorder(this),
        fileNamer = RecordingFileNamer(SystemClock()),
        directoryProvider = recordingDirectoryProvider
    )

    /** Reads the current list of recorded files from disk, including duration, and deletes them. Type: RecordingsRepository */
    private val recordingsRepository = RecordingsRepository(
        directoryProvider = recordingDirectoryProvider,
        audioDurationReader = MediaMetadataRetrieverDurationReader()
    )

    /** Builds each recording's display text. Type: RecordingDisplayFormatter */
    private val recordingDisplayFormatter = RecordingDisplayFormatter(
        dateFormatter = SystemDateFormatter(),
        durationFormatter = DurationFormatter()
    )

    /** Drives playback of a tapped recording, including stopping/switching tracks. Type: PlaybackController */
    private val playbackController = PlaybackController(
        audioPlayer = MediaPlayerAudioPlayer(),
        onPlaybackStateChanged = { previousFilePath, currentFilePath ->
            refreshPlaybackUi(previousFilePath, currentFilePath)
        }
    )

    /** Displays recordings inside rvRecordings and reports Play/Stop/Delete taps. Type: RecordingsAdapter */
    private val recordingsAdapter = RecordingsAdapter(
        displayFormatter = recordingDisplayFormatter,
        onPlayStopClicked = { recording -> playbackController.onItemClicked(recording.filePath) },
        onDeleteClicked = { recording -> onDeleteRecordingClicked(recording) },
        isPlaying = { filePath -> playbackController.isPlaying(filePath) }
    )

    private lateinit var btnToggleRecording: Button
    private lateinit var tvRecordingStatus: TextView
    private lateinit var rvRecordings: RecyclerView

    private val requestAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            onAudioPermissionResult(isGranted)
        }

    /**
     * @param savedInstanceState Previously saved state, or null on first creation. Type: Bundle?
     * @return Unit
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnToggleRecording = findViewById(R.id.btnToggleRecording)
        tvRecordingStatus = findViewById(R.id.tvStatus)
        rvRecordings = findViewById(R.id.rvRecordings)

        btnToggleRecording.setOnClickListener { onToggleRecordingClicked() }
        rvRecordings.layoutManager = LinearLayoutManager(this)
        rvRecordings.adapter = recordingsAdapter

        refreshRecordingUi()
        refreshRecordingsList()
        requestAudioPermissionIfNeeded()
    }

    /**
     * Stops any in-progress playback when the Activity is destroyed, so the
     * MediaPlayer doesn't keep running or leak after the screen is gone.
     *
     * @return Unit
     */
    override fun onDestroy() {
        playbackController.stopPlayback()
        super.onDestroy()
    }

    /**
     * Delegates the toggle button tap to recordingUiController, then starts
     * or stops the actual recording session to match the new UI state.
     * Refuses to start recording if RECORD_AUDIO has not been granted yet.
     *
     * @return Unit
     */
    private fun onToggleRecordingClicked() {
        val isCurrentlyIdle = recordingUiController.currentState() == RecordingState.IDLE
        if (isCurrentlyIdle && audioPermissionManager.needsPermissionRequest()) {
            Log.w(PERMISSION_LOG_TAG, "Cannot start recording without RECORD_AUDIO permission")
            return
        }

        recordingUiController.onToggleRecordingClicked()
        when (recordingUiController.currentState()) {
            RecordingState.RECORDING -> recordingSessionController.startNewRecording()
            RecordingState.IDLE -> {
                val recordedFilePath = recordingSessionController.stopCurrentRecording()
                Log.d(RECORDING_LOG_TAG, "Recording saved to $recordedFilePath")
                refreshRecordingsList()
            }
        }
        refreshRecordingUi()
    }

    /**
     * Shows a confirmation dialog before deleting a recording, since
     * deletion cannot be undone.
     *
     * @param recording Recording the user tapped Delete on. Type: RecordingInfo
     * @return Unit
     */
    private fun onDeleteRecordingClicked(recording: RecordingInfo) {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_recording_title)
            .setMessage(getString(R.string.delete_recording_message, recording.fileName))
            .setPositiveButton(R.string.delete) { _, _ -> deleteRecording(recording) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Stops playback if the file being deleted is currently playing, then
     * removes it from disk and refreshes the list.
     *
     * @param recording Recording to delete. Type: RecordingInfo
     * @return Unit
     */
    private fun deleteRecording(recording: RecordingInfo) {
        if (playbackController.isPlaying(recording.filePath)) {
            playbackController.stopPlayback()
        }
        recordingsRepository.deleteRecording(recording.filePath)
        refreshRecordingsList()
    }

    /**
     * @return Unit
     */
    private fun refreshRecordingUi() {
        btnToggleRecording.text = recordingUiController.toggleRecordingButtonLabel()
        tvRecordingStatus.text = recordingUiController.statusText()
    }

    /**
     * Reloads the list of recordings from disk and updates rvRecordings.
     *
     * @return Unit
     */
    private fun refreshRecordingsList() {
        recordingsAdapter.submitList(recordingsRepository.listRecordings())
    }

    /**
     * Refreshes only the rows affected by a playback state change, instead
     * of redrawing the whole list.
     *
     * @param previousFilePath File that was playing before the change, or null. Type: String?
     * @param currentFilePath File that is playing now, or null. Type: String?
     * @return Unit
     */
    private fun refreshPlaybackUi(previousFilePath: String?, currentFilePath: String?) {
        recordingsAdapter.notifyPlaybackChanged(previousFilePath, currentFilePath)
    }

    /**
     * @return Unit
     */
    private fun requestAudioPermissionIfNeeded() {
        if (audioPermissionManager.needsPermissionRequest()) {
            requestAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        } else {
            Log.d(PERMISSION_LOG_TAG, "RECORD_AUDIO already granted")
        }
    }

    /**
     * @param isGranted True if permission was granted, false if denied. Type: Boolean
     * @return Unit
     */
    private fun onAudioPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            Log.d(PERMISSION_LOG_TAG, "RECORD_AUDIO granted")
        } else {
            Log.d(PERMISSION_LOG_TAG, "RECORD_AUDIO denied")
        }
    }

    companion object {
        private const val PERMISSION_LOG_TAG = "Permissions"
        private const val RECORDING_LOG_TAG = "Recording"
    }
}