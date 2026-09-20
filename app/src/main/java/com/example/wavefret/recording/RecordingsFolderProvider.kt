package com.example.wavefret.recording

import com.example.wavefret.common.storage.AppStorageDirectoryProvider
import java.io.File

/**
 * Real RecordingDirectoryProvider that stores recordings in a "recordings"
 * subfolder of the app's base storage directory, creating it if needed.
 *
 * @property appStorageDirectoryProvider Resolves the app's base storage directory. Type: AppStorageDirectoryProvider
 */
class RecordingsFolderProvider(
    private val appStorageDirectoryProvider: AppStorageDirectoryProvider
) : RecordingDirectoryProvider {

    /** @return Absolute path to the "recordings" subfolder, created if needed. Type: String */
    override fun recordingsDirectoryPath(): String {
        val recordingsFolder = File(appStorageDirectoryProvider.baseDirectoryPath(), "recordings")
        if (!recordingsFolder.exists()) {
            recordingsFolder.mkdirs()
        }
        return recordingsFolder.absolutePath
    }
}