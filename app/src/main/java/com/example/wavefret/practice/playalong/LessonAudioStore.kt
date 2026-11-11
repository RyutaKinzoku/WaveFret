package com.example.wavefret.practice.playalong

import com.example.wavefret.common.storage.AppStorageDirectoryProvider
import java.io.File
import java.io.InputStream
import java.util.UUID

/**
 * Keeps a copy of the audio a lesson was transcribed from, in the app's
 * "lesson_audio" folder, so play-along keeps working even if the original
 * file is moved or the sharing app's access expires.
 *
 * @property appStorageDirectoryProvider Root of the app's storage. Type: AppStorageDirectoryProvider
 */
class LessonAudioStore(private val appStorageDirectoryProvider: AppStorageDirectoryProvider) {

    /**
     * @param originalFileName Name of the shared file, used only for its extension. Type: String
     * @param audioStream The audio's bytes; read to the end, not closed. Type: InputStream
     * @return The stored file's name. Type: String
     */
    fun store(originalFileName: String, audioStream: InputStream): String {
        val extension = originalFileName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase().takeIf { it.matches(SAFE_EXTENSION) } ?: DEFAULT_EXTENSION
        val storedName = "${UUID.randomUUID()}.$extension"
        fileFor(storedName).outputStream().use { audioStream.copyTo(it) }
        return storedName
    }

    /**
     * @param storedName Name returned by [store]. Type: String
     * @return The stored file; any directory part of the name is ignored. Type: File
     */
    fun fileFor(storedName: String): File = File(audioFolder(), File(storedName).name)

    /**
     * @param storedName Name returned by [store]. Type: String
     * @return Unit
     */
    fun delete(storedName: String) {
        fileFor(storedName).delete()
    }

    /** @return The folder, created on first use. Type: File */
    private fun audioFolder(): File = File(appStorageDirectoryProvider.baseDirectoryPath(), FOLDER_NAME).apply { mkdirs() }

    private companion object {
        const val FOLDER_NAME = "lesson_audio"
        const val DEFAULT_EXTENSION = "audio"
        val SAFE_EXTENSION = Regex("[a-z0-9]{1,5}")
    }
}
