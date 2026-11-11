package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTuning
import com.example.wavefret.practice.TabNote

/**
 * Reads the ASCII bass tabs commonly shared online:
 *
 * ```
 * G|-------------|
 * D|-------------|
 * A|-----0--2--3-|
 * E|--3----------|
 * ```
 *
 * A "system" is one line per string of the tuning, consecutively. Notes
 * are read left to right by column; notes in the same column are ordered
 * low string first. Any text between systems (titles, lyrics, chord
 * names) is ignored, and technique marks (h, p, /, ~ …) are skipped, so
 * only the fretted notes remain. Rhythm is not encoded in ASCII tab, so
 * it is not read.
 *
 * @property tuning Tuning whose string names and count the tab uses. Type: BassTuning
 */
class AsciiTabParser(private val tuning: BassTuning) {

    /**
     * @param asciiTabText Full pasted tab text. Type: String
     * @return Every note in the tab, in playing order. Type: List<TabNote>
     * @throws IllegalArgumentException If no tab system or no notes are found.
     */
    fun parseNotes(asciiTabText: String): List<TabNote> {
        val tabSystems = findTabSystems(asciiTabText.lines())
        require(tabSystems.isNotEmpty()) { "No ${tuning.strings.size}-line tab system found" }
        val notes = tabSystems.flatMap(::notesInSystem)
        require(notes.isNotEmpty()) { "The tab contains no fretted notes" }
        return notes
    }

    /**
     * Groups consecutive tab lines into systems of exactly one line per string.
     *
     * @param lines All lines of the pasted text. Type: List<String>
     * @return Each complete system, top line first. Type: List<List<String>>
     */
    private fun findTabSystems(lines: List<String>): List<List<String>> {
        val stringCount = tuning.strings.size
        val tabSystems = mutableListOf<List<String>>()
        val pendingSystemLines = mutableListOf<String>()
        for (line in lines) {
            if (TAB_LINE_PATTERN.matches(line)) {
                pendingSystemLines += line
                if (pendingSystemLines.size == stringCount) {
                    tabSystems += pendingSystemLines.toList()
                    pendingSystemLines.clear()
                }
            } else {
                pendingSystemLines.clear()
            }
        }
        return tabSystems
    }

    /**
     * @param systemLines One line per string, top (highest) string first. Type: List<String>
     * @return The system's notes ordered by column, then low string first. Type: List<TabNote>
     */
    private fun notesInSystem(systemLines: List<String>): List<TabNote> {
        val positionedNotes = systemLines.flatMapIndexed { lineIndex, line ->
            val lineMatch = requireNotNull(TAB_LINE_PATTERN.matchEntire(line))
            val stringIndex = stringIndexFor(lineMatch.groupValues[1], lineIndex)
            val content = lineMatch.groupValues[2]
            FRET_NUMBER_PATTERN.findAll(content).map { fretMatch ->
                PositionedNote(column = fretMatch.range.first, TabNote(stringIndex, fretMatch.value.toInt()))
            }.toList()
        }
        return positionedNotes
            .sortedWith(compareBy({ it.column }, { it.tabNote.stringIndex }))
            .map { it.tabNote }
    }

    /**
     * Resolves a line's string by its name when that name is unique in the
     * tuning, otherwise by its position (top line = highest string).
     *
     * @param stringName Name written at the start of the line. Type: String
     * @param lineIndex Line's position in the system, 0 being the top. Type: Int
     * @return Zero-based string index, 0 being the lowest string. Type: Int
     */
    private fun stringIndexFor(stringName: String, lineIndex: Int): Int {
        val matchingIndices = tuning.strings.indices.filter { tuning.strings[it].name.equals(stringName, ignoreCase = true) }
        return matchingIndices.singleOrNull() ?: (tuning.strings.size - 1 - lineIndex)
    }

    /**
     * A note together with the text column it was written in.
     *
     * @property column Column of the fret number's first digit. Type: Int
     * @property tabNote The note itself. Type: TabNote
     */
    private data class PositionedNote(val column: Int, val tabNote: TabNote)

    private companion object {
        /** A string name, an optional bar, then at least three dashes' worth of tab. */
        val TAB_LINE_PATTERN = Regex("^\\s*([A-Ga-g][#b]?)\\s*[|:]?((?=(?:[^-]*-){3})[-0-9|hpbsrx~/\\\\.()^*<>=\\s]+)$")
        val FRET_NUMBER_PATTERN = Regex("\\d+")
    }
}
