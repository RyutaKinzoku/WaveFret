package com.example.wavefret.recording

/**
 * Orchestrates a single recording session: builds the output file path and
 * drives the underlying AudioRecorder's lifecycle (start, stop, release).
 * Single responsibility: session bookkeeping, independent of both the UI
 * and the concrete recording technology.
 *
 * @property audioRecorder Platform recorder abstraction to start/stop. Type: AudioRecorder
 * @property fileNamer Generates unique file names for new recordings. Type: RecordingFileNamer
 * @property directoryProvider Resolves where recording files should be stored. Type: RecordingDirectoryProvider
 */
class RecordingSessionController(
    private val audioRecorder: AudioRecorder,
    private val fileNamer: RecordingFileNamer,
    private val directoryProvider: RecordingDirectoryProvider
) {

    private var activeRecordingFilePath: String? = null

    /**
     * Starts a new recording: builds an output path and delegates to audioRecorder.
     *
     * @return Unit
     */
    fun startNewRecording() {
        val filePath = "${directoryProvider.recordingsDirectoryPath()}/${fileNamer.generateFileName()}"
        activeRecordingFilePath = filePath
        audioRecorder.startRecordingToFile(filePath)
    }

    /**
     * Stops the current recording, if any, and releases the recorder.
     *
     * @return Absolute path of the file that was just recorded, or null if
     *   no recording was in progress. Type: String?
     */
    fun stopCurrentRecording(): String? {
        val filePath = activeRecordingFilePath ?: return null
        audioRecorder.stopRecording()
        audioRecorder.releaseRecorder()
        activeRecordingFilePath = null
        return filePath
    }
}