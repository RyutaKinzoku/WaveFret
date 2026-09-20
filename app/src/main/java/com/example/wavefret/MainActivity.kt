package com.example.wavefret

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.wavefret.permission.AudioPermissionManager
import com.example.wavefret.permission.SystemPermissionChecker
import com.example.wavefret.recording.ExternalFilesRecordingDirectoryProvider
import com.example.wavefret.recording.MediaRecorderAudioRecorder
import com.example.wavefret.recording.RecordingFileNamer
import com.example.wavefret.recording.RecordingSessionController
import com.example.wavefret.recording.RecordingState
import com.example.wavefret.recording.RecordingUiController
import com.example.wavefret.recording.SystemClock

/**
 * App entry point. Sets up edge-to-edge layout, requests microphone access,
 * and wires the record/stop toggle button to RecordingUiController and
 * RecordingSessionController.
 */
class MainActivity : AppCompatActivity() {

    /** Decides whether RECORD_AUDIO needs to be requested. Type: AudioPermissionManager */
    private val audioPermissionManager = AudioPermissionManager(SystemPermissionChecker(this))

    /** Holds recording UI state (button label, status text), independent of Android Views. Type: RecordingUiController */
    private val recordingUiController = RecordingUiController()

    /** Drives the actual MediaRecorder lifecycle for each recording session. Type: RecordingSessionController */
    private val recordingSessionController = RecordingSessionController(
        audioRecorder = MediaRecorderAudioRecorder(this),
        fileNamer = RecordingFileNamer(SystemClock()),
        directoryProvider = ExternalFilesRecordingDirectoryProvider(this)
    )

    private lateinit var btnToggleRecording: Button
    private lateinit var tvRecordingStatus: TextView

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

        btnToggleRecording.setOnClickListener { onToggleRecordingClicked() }

        refreshRecordingUi()
        requestAudioPermissionIfNeeded()
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
            }
        }
        refreshRecordingUi()
    }

    /**
     * Updates the toggle button's label and the status text to match
     * recordingUiController's current state.
     *
     * @return Unit
     */
    private fun refreshRecordingUi() {
        btnToggleRecording.text = recordingUiController.toggleRecordingButtonLabel()
        tvRecordingStatus.text = recordingUiController.statusText()
    }

    /**
     * Requests RECORD_AUDIO from the user if audioPermissionManager determines
     * it is not currently granted.
     *
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