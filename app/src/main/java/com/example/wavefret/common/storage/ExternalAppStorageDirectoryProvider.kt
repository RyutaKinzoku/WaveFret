package com.example.wavefret.common.storage

import android.content.Context

/**
 * Real AppStorageDirectoryProvider backed by the app's external files
 * directory, falling back to internal storage if unavailable.
 *
 * @property context Used to resolve the external files directory. Type: Context
 */
class ExternalAppStorageDirectoryProvider(private val context: Context) : AppStorageDirectoryProvider {

    /** @return Absolute path to the app's external files directory, or internal storage as fallback. Type: String */
    override fun baseDirectoryPath(): String {
        return context.getExternalFilesDir(null)?.absolutePath ?: context.filesDir.absolutePath
    }
}