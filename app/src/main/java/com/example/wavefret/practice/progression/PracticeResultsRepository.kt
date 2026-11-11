package com.example.wavefret.practice.progression

import com.example.wavefret.common.storage.AppStorageDirectoryProvider
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Appends every completed practice run to a tab-separated file in app
 * storage, one line per run, so history survives restarts and can later be
 * exported as a dataset. Titles are URL-encoded so tabs and newlines in
 * them can't break the format.
 *
 * @property appStorageDirectoryProvider Root of the app's storage. Type: AppStorageDirectoryProvider
 */
class PracticeResultsRepository(private val appStorageDirectoryProvider: AppStorageDirectoryProvider) {

    /**
     * @param practiceResult Run to append. Type: PracticeResult
     * @return Unit
     */
    fun record(practiceResult: PracticeResult) {
        val fields = listOf(
            URLEncoder.encode(practiceResult.exerciseTitle, Charsets.UTF_8.name()),
            practiceResult.practiceMode.name,
            practiceResult.scorePercent,
            practiceResult.hitCount,
            practiceResult.missCount,
            practiceResult.completedAtMillis
        )
        resultsFile().appendText(fields.joinToString(FIELD_SEPARATOR) + "\n")
    }

    /**
     * @return Every recorded run, oldest first; unreadable lines are skipped. Type: List<PracticeResult>
     */
    fun listResults(): List<PracticeResult> {
        val resultsFile = resultsFile()
        if (!resultsFile.exists()) return emptyList()
        return resultsFile.readLines().mapNotNull(::parseLineOrNull)
    }

    /**
     * @param line One stored line. Type: String
     * @return The run it describes, or null if the line is malformed. Type: PracticeResult?
     */
    private fun parseLineOrNull(line: String): PracticeResult? {
        val fields = line.split(FIELD_SEPARATOR)
        if (fields.size != FIELD_COUNT) return null
        return try {
            PracticeResult(
                exerciseTitle = URLDecoder.decode(fields[0], Charsets.UTF_8.name()),
                practiceMode = PracticeMode.valueOf(fields[1]),
                scorePercent = fields[2].toInt(),
                hitCount = fields[3].toInt(),
                missCount = fields[4].toInt(),
                completedAtMillis = fields[5].toLong()
            )
        } catch (malformedField: IllegalArgumentException) {
            null
        }
    }

    /** @return The results file in app storage. Type: File */
    private fun resultsFile(): File = File(appStorageDirectoryProvider.baseDirectoryPath(), FILE_NAME)

    private companion object {
        const val FILE_NAME = "practice_results.tsv"
        const val FIELD_SEPARATOR = "\t"
        const val FIELD_COUNT = 6
    }
}
