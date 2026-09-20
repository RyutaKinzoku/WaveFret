package com.example.wavefret.recording

/**
 * Abstraction over where recording files should be stored, so orchestration
 * logic can be unit tested without a real Android Context.
 */
interface RecordingDirectoryProvider {
    /** @return Absolute path to the directory where recordings should be saved. Type: String */
    fun recordingsDirectoryPath(): String
}