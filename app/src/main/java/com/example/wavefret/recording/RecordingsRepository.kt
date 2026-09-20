package com.example.wavefret.recording

import java.io.File

/**
 * Reads recorded audio files from the recordings directory and can delete
 * them. Single responsibility: reflect what's on disk as a list of
 * RecordingInfo (newest first), and manage each file's lifecycle there.
 *
 * @property directoryProvider Resolves where recording files are stored. Type: RecordingDirectoryProvider
 * @property audioDurationReader Reads each recording's duration. Type: AudioDurationReader
 */
class RecordingsRepository(
    private val directoryProvider: RecordingDirectoryProvider,
    private val audioDurationReader: AudioDurationReader
) {

    /**
     * @return All .m4a recordings in the recordings directory, sorted by
     *   lastModifiedMillis descending (most recent first). Type: List<RecordingInfo>
     */
    fun listRecordings(): List<RecordingInfo> {
        val directory = File(directoryProvider.recordingsDirectoryPath())
        val files = directory.listFiles { file -> file.extension == "m4a" } ?: emptyArray()
        return files
            .map { file ->
                RecordingInfo(
                    filePath = file.absolutePath,
                    fileName = file.name,
                    lastModifiedMillis = file.lastModified(),
                    durationMillis = audioDurationReader.readDurationMillis(file.absolutePath)
                )
            }
            .sortedByDescending { it.lastModifiedMillis }
    }

    /**
     * Deletes a recording file from disk.
     *
     * @param filePath Absolute path of the file to delete. Type: String
     * @return True if the file existed and was deleted, false otherwise. Type: Boolean
     */
    fun deleteRecording(filePath: String): Boolean {
        return File(filePath).delete()
    }
}