package com.example.wavefret.recording

import java.io.File

/**
 * Reads recorded audio files from the recordings directory.
 * Single responsibility: turn what's on disk into a list of RecordingInfo,
 * newest recordings first.
 *
 * @property directoryProvider Resolves where recording files are stored. Type: RecordingDirectoryProvider
 */
class RecordingsRepository(private val directoryProvider: RecordingDirectoryProvider) {

    /**
     * @return All .m4a recordings in the recordings directory, sorted by
     *   lastModifiedMillis descending (most recent first). Type: List<RecordingInfo>
     */
    fun listRecordings(): List<RecordingInfo> {
        val directory = File(directoryProvider.recordingsDirectoryPath())
        val files = directory.listFiles { file -> file.extension == "m4a" } ?: emptyArray()
        return files
            .map { file -> RecordingInfo(file.absolutePath, file.name, file.lastModified()) }
            .sortedByDescending { it.lastModifiedMillis }
    }
}