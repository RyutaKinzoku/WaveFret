package com.example.wavefret.recording

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
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
import java.io.File

/**
 * Recorder: one large record/stop button in the thumb zone, a running
 * timer while recording, and the list of takes with play, share and
 * delete. The camera switch in the app bar turns on a live preview (front
 * or back camera) so takes are recorded as video with sound. Deleting asks
 * for confirmation, and leaving the screen mid-take saves it instead of
 * losing it. Sharing opens Android's share sheet so the player chooses the
 * app and the person.
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
            onPlayStopClicked = ::onPlayTakeClicked,
            onDeleteClicked = ::confirmDeleteRecording,
            onShareClicked = { recording -> recordingSharer.share(recording) },
            onMakeLessonClicked = { recording -> startActivity(recordingSharer.makeLessonIntent(recording)) },
            isPlaying = playbackController::isPlaying
        )
    }

    private lateinit var rootView: View
    private lateinit var timerTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var takesRecyclerView: RecyclerView
    private lateinit var emptyStateView: View
    private lateinit var toggleButton: FloatingActionButton
    private lateinit var cameraCard: View
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var topAppBar: MaterialToolbar

    private val recordingSharer by lazy { RecordingSharer(this) }
    private val cameraXVideoRecorder by lazy { CameraXVideoRecorder(this) }
    private val recordingFileNamer by lazy { RecordingFileNamer(appContainer.clock) }
    private var isCameraOn = false
    private var useFrontCamera = true

    private var timerJob: Job? = null

    private val requestMicrophonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startRecording() else showMicrophonePermissionNeeded(rootView, R.string.recorder_permission_needed)
    }

    private val requestCameraPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        if (grantResults.values.all { it }) {
            turnCameraOn()
        } else {
            showMicrophonePermissionNeeded(rootView, R.string.recorder_camera_permission_needed)
        }
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

        cameraCard = findViewById(R.id.recorderCameraCard)
        cameraPreviewView = findViewById(R.id.recorderCameraPreviewView)
        topAppBar = findViewById(R.id.topAppBar)
        topAppBar.setTitle(R.string.recorder_title)
        topAppBar.setNavigationOnClickListener { finish() }
        topAppBar.inflateMenu(R.menu.menu_recorder)
        topAppBar.setOnMenuItemClickListener { menuItem ->
            val isCameraItem = menuItem.itemId == R.id.recorderCameraMenuItem
            if (isCameraItem) onCameraToggleClicked()
            isCameraItem
        }
        findViewById<View>(R.id.recorderFlipCameraButton).setOnClickListener {
            useFrontCamera = !useFrontCamera
            cameraXVideoRecorder.startPreview(this, cameraPreviewView, useFrontCamera)
        }
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
     * Re-opens the camera preview when coming back with the camera on.
     *
     * @return Unit
     */
    override fun onResume() {
        super.onResume()
        if (isCameraOn) cameraXVideoRecorder.startPreview(this, cameraPreviewView, useFrontCamera)
    }

    /**
     * @return Unit
     */
    private fun onCameraToggleClicked() {
        if (recordingUiController.currentState() == RecordingState.RECORDING) return
        when {
            isCameraOn -> turnCameraOff()
            hasPermission(Manifest.permission.CAMERA) && hasPermission(Manifest.permission.RECORD_AUDIO) -> turnCameraOn()
            else -> requestCameraPermissionsLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    /**
     * @return Unit
     */
    private fun turnCameraOn() {
        isCameraOn = true
        cameraCard.isVisible = true
        cameraXVideoRecorder.startPreview(this, cameraPreviewView, useFrontCamera)
        renderRecordingState()
    }

    /**
     * @return Unit
     */
    private fun turnCameraOff() {
        isCameraOn = false
        cameraXVideoRecorder.release()
        cameraCard.isVisible = false
        renderRecordingState()
    }

    /**
     * @param permission Permission to check. Type: String
     * @return True if it is granted. Type: Boolean
     */
    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    /**
     * Audio takes play inline; video takes open the video player.
     *
     * @param recording Take the player tapped. Type: RecordingInfo
     * @return Unit
     */
    private fun onPlayTakeClicked(recording: RecordingInfo) {
        if (recording.mediaType == RecordingMediaType.VIDEO) {
            playbackController.stopPlayback()
            startActivity(
                VideoPlayerActivity.intentFor(this, recording.filePath, recordingDisplayFormatter.formatTitle(recording))
            )
        } else {
            playbackController.onItemClicked(recording.filePath)
        }
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
        if (isCameraOn) {
            if (!hasPermission(Manifest.permission.RECORD_AUDIO)) return
            val videoFile = File(
                recordingDirectoryProvider.recordingsDirectoryPath(),
                recordingFileNamer.generateFileName(RecordingMediaType.VIDEO)
            )
            val hasStarted = try {
                cameraXVideoRecorder.startRecording(videoFile, ::onVideoTakeFinished)
            } catch (missingPermission: SecurityException) {
                false
            }
            if (!hasStarted) return
        } else {
            recordingSessionController.startNewRecording()
        }
        recordingUiController.onRecordClicked()
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
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        renderRecordingState()
        if (isCameraOn) {
            cameraXVideoRecorder.stopRecording()
        } else {
            recordingSessionController.stopCurrentRecording()
            onTakeSaved()
        }
    }

    /**
     * @param isSaved Whether CameraX finished writing the video file. Type: Boolean
     * @return Unit
     */
    private fun onVideoTakeFinished(isSaved: Boolean) {
        if (isSaved) {
            onTakeSaved()
        } else {
            Snackbar.make(rootView, R.string.recorder_video_failed, Snackbar.LENGTH_LONG).setAnchorView(toggleButton).show()
            refreshTakes()
        }
    }

    /**
     * @return Unit
     */
    private fun onTakeSaved() {
        refreshTakes()
        Snackbar.make(rootView, R.string.recording_saved, Snackbar.LENGTH_SHORT).setAnchorView(toggleButton).show()
    }

    /**
     * @return Unit
     */
    private fun renderRecordingState() {
        val isRecording = recordingUiController.currentState() == RecordingState.RECORDING
        statusTextView.setText(
            when {
                isRecording -> R.string.recorder_status_recording
                isCameraOn -> R.string.recorder_status_idle_video
                else -> R.string.recorder_status_idle
            }
        )
        toggleButton.setImageResource(
            when {
                isRecording -> R.drawable.ic_stop
                isCameraOn -> R.drawable.ic_videocam
                else -> R.drawable.ic_mic
            }
        )
        toggleButton.contentDescription = getString(
            when {
                isRecording -> R.string.recorder_stop_description
                isCameraOn -> R.string.recorder_start_video_description
                else -> R.string.recorder_start_description
            }
        )
        topAppBar.menu.findItem(R.id.recorderCameraMenuItem)?.apply {
            setIcon(if (isCameraOn) R.drawable.ic_videocam_off else R.drawable.ic_videocam)
            setTitle(if (isCameraOn) R.string.recorder_camera_off else R.string.recorder_camera_on)
            isEnabled = !isRecording
        }
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
