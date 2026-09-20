package com.example.wavefret.recording

/**
 * Abstraction over the platform audio-recording API, so recording
 * orchestration logic can be unit tested without an Android framework
 * dependency or real microphone access.
 */
interface AudioRecorder {
    /**
     * @param outputFilePath Absolute path where the recorded audio file will be written. Type: String
     * @return Unit
     */
    fun startRecordingToFile(outputFilePath: String)

    /** @return Unit */
    fun stopRecording()

    /** @return Unit */
    fun releaseRecorder()
}