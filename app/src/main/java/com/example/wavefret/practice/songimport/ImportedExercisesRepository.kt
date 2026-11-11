package com.example.wavefret.practice.songimport

import com.example.wavefret.common.storage.AppStorageDirectoryProvider
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.TabTextParser
import com.example.wavefret.practice.TabTextWriter
import java.io.File
import java.net.URLEncoder

/**
 * Stores imported songs as WaveFret tab-text files, one per title, in an
 * "imported_tabs" folder of app storage, so they survive restarts and
 * stay human-readable.
 *
 * @property appStorageDirectoryProvider Root of the app's storage. Type: AppStorageDirectoryProvider
 * @property tabTextWriter Turns an exercise into file text. Type: TabTextWriter
 * @property tabTextParser Turns file text back into an exercise. Type: TabTextParser
 */
class ImportedExercisesRepository(
    private val appStorageDirectoryProvider: AppStorageDirectoryProvider,
    private val tabTextWriter: TabTextWriter,
    private val tabTextParser: TabTextParser
) {

    /**
     * @return Every stored exercise, sorted by title; unreadable files are skipped. Type: List<Exercise>
     */
    fun listExercises(): List<Exercise> {
        val storedFiles = importedTabsFolder().listFiles { file -> file.name.endsWith(FILE_SUFFIX) } ?: return emptyList()
        return storedFiles
            .mapNotNull { file -> readExerciseOrNull(file) }
            .sortedBy { it.title.lowercase() }
    }

    /**
     * Saves an exercise, replacing any stored one with the same title.
     *
     * @param exercise Exercise to store. Type: Exercise
     * @return Unit
     */
    fun save(exercise: Exercise) {
        fileFor(exercise.title).writeText(tabTextWriter.write(exercise))
    }

    /**
     * @param title Title of the exercise to remove. Type: String
     * @return Unit
     */
    fun delete(title: String) {
        fileFor(title).delete()
    }

    /**
     * @param file A stored tab file. Type: File
     * @return The exercise it holds, or null if it can't be parsed. Type: Exercise?
     */
    private fun readExerciseOrNull(file: File): Exercise? = try {
        tabTextParser.parse(file.readText())
    } catch (unreadableTab: IllegalArgumentException) {
        null
    }

    /**
     * @param title Exercise title. Type: String
     * @return The file that stores it; the title is URL-encoded to be a safe file name. Type: File
     */
    private fun fileFor(title: String): File {
        return File(importedTabsFolder(), URLEncoder.encode(title, Charsets.UTF_8.name()) + FILE_SUFFIX)
    }

    /**
     * @return The storage folder, created on first use. Type: File
     */
    private fun importedTabsFolder(): File {
        return File(appStorageDirectoryProvider.baseDirectoryPath(), FOLDER_NAME).apply { mkdirs() }
    }

    private companion object {
        const val FOLDER_NAME = "imported_tabs"
        const val FILE_SUFFIX = ".wavefret.txt"
    }
}
