package com.example.wavefret.practice.songimport

import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.TabTextParser

/**
 * Turns pasted text into an [Exercise], detecting whether it is an ASCII
 * tab from the internet or WaveFret's own tab text. A title or tempo given
 * by the player overrides anything found in the text.
 *
 * @property asciiTabParser Reads internet-style ASCII tabs. Type: AsciiTabParser
 * @property tabTextParser Reads WaveFret's own tab format. Type: TabTextParser
 */
class SongImporter(
    private val asciiTabParser: AsciiTabParser,
    private val tabTextParser: TabTextParser
) {

    /**
     * @param pastedText Text the player pasted. Type: String
     * @param title Title to use, or blank to keep the text's own/default title. Type: String
     * @param tempoBpm Tempo to use, or null to keep the text's own/default tempo. Type: Int?
     * @return The imported exercise. Type: Exercise
     * @throws IllegalArgumentException If the text is in neither supported format.
     */
    fun import(pastedText: String, title: String, tempoBpm: Int?): Exercise {
        val parsedExercise = parseAsciiTabOrNull(pastedText)
            ?: parseTabTextOrNull(pastedText)
            ?: throw IllegalArgumentException("Text is neither an ASCII bass tab nor WaveFret tab text")
        return parsedExercise.copy(
            title = title.trim().ifEmpty { parsedExercise.title },
            tempoBpm = tempoBpm ?: parsedExercise.tempoBpm
        )
    }

    /**
     * @param pastedText Text to try. Type: String
     * @return The exercise if the text is an ASCII tab, otherwise null. Type: Exercise?
     */
    private fun parseAsciiTabOrNull(pastedText: String): Exercise? = try {
        Exercise(
            title = DEFAULT_IMPORTED_TITLE,
            tuning = asciiTabParser.tuning,
            tempoBpm = TabTextParser.DEFAULT_TEMPO_BPM,
            notes = asciiTabParser.parseNotes(pastedText)
        )
    } catch (notAsciiTab: IllegalArgumentException) {
        null
    }

    /**
     * @param pastedText Text to try. Type: String
     * @return The exercise if the text is WaveFret tab text, otherwise null. Type: Exercise?
     */
    private fun parseTabTextOrNull(pastedText: String): Exercise? = try {
        tabTextParser.parse(pastedText)
    } catch (notTabText: IllegalArgumentException) {
        null
    }

    companion object {
        /** Title given to an imported tab when neither the player nor the text names it. Type: String */
        const val DEFAULT_IMPORTED_TITLE = "Imported song"
    }
}
