package com.example.wavefret.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log

/**
 * Real AudioRecorder implementation backed by android.media.MediaRecorder,
 * configured to record microphone input as AAC audio in an MPEG-4 container.
 *
 * @property context Required to construct MediaRecorder on API 31+. Type: Context
 */
class MediaRecorderAudioRecorder(private val context: Context) : AudioRecorder {

    private var mediaRecorder: MediaRecorder? = null

    /**
     * @param outputFilePath Absolute path where the recorded audio file will be written. Type: String
     * @return Unit
     */
    override fun startRecordingToFile(outputFilePath: String) {
        val recorder = createPlatformMediaRecorder()
        try {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFilePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording to $outputFilePath", e)
        }
    }

    /** @return Unit */
    override fun stopRecording() {
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
        }
    }

    /** @return Unit */
    override fun releaseRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
    }

    /**
     * @return A new platform MediaRecorder instance, using the constructor
     *   required for the running API level. Type: MediaRecorder
     */
    private fun createPlatformMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    companion object {
        private const val TAG = "MediaRecorderAudioRecorder"
    }
}