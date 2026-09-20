package com.example.wavefret.recording

import android.content.Context

/**
 * Real RecordingDirectoryProvider backed by the app's external files
 * directory (app-private storage, visible over USB, no storage permission needed).
 *
 * @property context Used to resolve the external files directory. Type: Context
 */
class ExternalFilesRecordingDirectoryProvider(private val context: Context) : RecordingDirectoryProvider {

    /**
     * @return Absolute path to the app's external files directory, or
     *   internal storage as a fallback if external storage is unavailable. Type: String
     */
    override fun recordingsDirectoryPath(): String {
        return context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
    }
}