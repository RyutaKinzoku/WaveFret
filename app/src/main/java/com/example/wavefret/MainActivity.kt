package com.example.wavefret

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.wavefret.permission.AudioPermissionManager
import com.example.wavefret.permission.SystemPermissionChecker

/**
 * App entry point. Sets up edge-to-edge layout and requests microphone
 * access needed for the recording/pitch-detection features.
 */
class MainActivity : AppCompatActivity() {

    /** Decides whether RECORD_AUDIO needs to be requested. Type: AudioPermissionManager */
    private val audioPermissionManager = AudioPermissionManager(SystemPermissionChecker(this))

    /** Launcher that shows the system RECORD_AUDIO dialog and reports the user's decision. */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            onPermissionResult(isGranted)
        }

    /**
     * Called by Android when the activity is created.
     *
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

        requestAudioPermissionIfNeeded()
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
     * Handles the user's response to the RECORD_AUDIO permission dialog.
     *
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