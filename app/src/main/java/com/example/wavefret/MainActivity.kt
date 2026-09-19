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
import com.example.wavefret.recording.RecordingUiController

/**
 * App entry point. Sets up edge-to-edge layout, requests microphone access,
 * and wires the Record/Stop buttons to RecordingUiController.
 */
class MainActivity : AppCompatActivity() {

    /** Decides whether RECORD_AUDIO needs to be requested. Type: AudioPermissionManager */
    private val audioPermissionManager = AudioPermissionManager(SystemPermissionChecker(this))

    /** Holds recording UI state, independent of Android Views. Type: RecordingUiController */
    private val recordingUiController = RecordingUiController()

    private lateinit var btnRecord: Button
    private lateinit var btnStop: Button
    private lateinit var tvStatus: TextView

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            onPermissionResult(isGranted)
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

        btnRecord = findViewById(R.id.btnRecord)
        btnStop = findViewById(R.id.btnStop)
        tvStatus = findViewById(R.id.tvStatus)

        btnRecord.setOnClickListener { onRecordClicked() }
        btnStop.setOnClickListener { onStopClicked() }

        refreshUi()
        requestAudioPermissionIfNeeded()
    }

    /**
     * Delegates the Record button tap to recordingUiController and refreshes the UI.
     *
     * @return Unit
     */
    private fun onRecordClicked() {
        recordingUiController.onRecordClicked()
        refreshUi()
    }

    /**
     * Delegates the Stop button tap to recordingUiController and refreshes the UI.
     *
     * @return Unit
     */
    private fun onStopClicked() {
        recordingUiController.onStopClicked()
        refreshUi()
    }

    /**
     * Updates button enabled-states and status text to match
     * recordingUiController's current state.
     *
     * @return Unit
     */
    private fun refreshUi() {
        btnRecord.isEnabled = recordingUiController.isRecordButtonEnabled()
        btnStop.isEnabled = recordingUiController.isStopButtonEnabled()
        tvStatus.text = recordingUiController.statusText()
    }

    /**
     * Requests RECORD_AUDIO from the user if audioPermissionManager determines
     * it is not currently granted.
     *
     * @return Unit
     */
    private fun requestAudioPermissionIfNeeded() {
        if (audioPermissionManager.needsPermissionRequest()) {
            requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        } else {
            Log.d(TAG, "RECORD_AUDIO already granted")
        }
    }

    /**
     * @param isGranted True if permission was granted, false if denied. Type: Boolean
     * @return Unit
     */
    private fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            Log.d(TAG, "RECORD_AUDIO granted")
        } else {
            Log.d(TAG, "RECORD_AUDIO denied")
        }
    }

    companion object {
        private const val TAG = "Permissions"
    }
}