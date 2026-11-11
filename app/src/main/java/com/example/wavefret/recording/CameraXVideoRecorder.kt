package com.example.wavefret.recording

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File

/**
 * Records video takes with sound through CameraX, showing a live preview.
 * The camera is bound to the screen's lifecycle, so it is released
 * automatically when the screen goes away.
 *
 * Thin wrapper over CameraX with no decision logic, so per this project's
 * convention it is verified on-device rather than in JUnit.
 *
 * @param context Context used to reach the camera service. Type: Context
 */
class CameraXVideoRecorder(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    /**
     * Opens the chosen camera and starts showing its preview.
     *
     * @param lifecycleOwner Screen whose lifecycle owns the camera. Type: LifecycleOwner
     * @param previewView Where the live picture is shown. Type: PreviewView
     * @param useFrontCamera True for the selfie camera, false for the back one. Type: Boolean
     * @return Unit
     */
    fun startPreview(lifecycleOwner: LifecycleOwner, previewView: PreviewView, useFrontCamera: Boolean) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build()
            val newVideoCapture = VideoCapture.withOutput(recorder)
            val cameraSelector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, newVideoCapture)
            videoCapture = newVideoCapture
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Starts recording a video take with sound.
     *
     * @param outputFile Where the .mp4 is written. Type: File
     * @param onFinished Called once the file is complete, with true on success. Type: (Boolean) -> Unit
     * @return True if recording started, false if the camera isn't ready yet. Type: Boolean
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(outputFile: File, onFinished: (Boolean) -> Unit): Boolean {
        val readyVideoCapture = videoCapture ?: return false
        activeRecording = readyVideoCapture.output
            .prepareRecording(context, FileOutputOptions.Builder(outputFile).build())
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { videoRecordEvent ->
                if (videoRecordEvent is VideoRecordEvent.Finalize) {
                    activeRecording = null
                    onFinished(!videoRecordEvent.hasError())
                }
            }
        return true
    }

    /**
     * Stops the current take; the finished callback fires when the file is complete.
     *
     * @return Unit
     */
    fun stopRecording() {
        activeRecording?.stop()
    }

    /**
     * Stops any take and closes the camera.
     *
     * @return Unit
     */
    fun release() {
        activeRecording?.stop()
        activeRecording = null
        cameraProvider?.unbindAll()
        videoCapture = null
    }
}
