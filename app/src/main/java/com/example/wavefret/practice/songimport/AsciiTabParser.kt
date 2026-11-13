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
 * only the fretted notes remain. Tabs copied from websites, PDFs or
 * photos are tolerated: typographic dashes, missing string names, dead
 * or ghost note letters, repeat counts after the last bar and 5-string
 * systems (the low B line is dropped). Rhythm is not encoded in ASCII
 * tab, so it is not read.
 *
 * @property tuning Tuning whose string names and count the tab uses. Type: BassTuning
 */
class AsciiTabParser(val tuning: BassTuning) {

    /**
     * @param asciiTabText Full pasted tab text. Type: String
     * @return Every note in the tab, in playing order. Type: List<TabNote>
     * @throws IllegalArgumentException If no tab system or no notes are found.
     */
    fun parseNotes(asciiTabText: String): List<TabNote> {
        val tabSystems = findTabSystems(asciiTabText.lines().map(::normalizedLine))
        require(tabSystems.isNotEmpty()) { "No ${tuning.strings.size}-line tab system found" }
        val notes = tabSystems.flatMap(::notesInSystem)
        require(notes.isNotEmpty()) { "The tab contains no fretted notes" }
        return notes
    }

    /**
     * Cuts the tab out of a longer text, such as everything on a web page:
     * from the first line of the first system to the last line of the last
     * system, keeping section names and blank lines in between.
     *
     * @param text Text that may contain a tab. Type: String
     * @return The tab part of the text, or null if it has no tab system. Type: String?
     */
    fun extractTab(text: String): String? {
        val lines = text.lines()
        val tabSystems = findTabSystems(lines.map(::normalizedLine))
        if (tabSystems.isEmpty()) return null
        val firstLineIndex = tabSystems.first().first().lineIndex
        val lastLineIndex = tabSystems.last().last().lineIndex
        return lines.subList(firstLineIndex, lastLineIndex + 1).joinToString("\n")
    }

    /**
     * Groups consecutive tab lines into systems of exactly one line per
     * string. A run of several systems written back to back is split; a
     * run with one or two extra lines (a 5- or 6-string tab) keeps its top
     * lines, which are the strings this tuning has.
     *
     * @param lines All lines of the pasted text, normalized. Type: List<String>
     * @return Each complete system, as parsed lines, top line first. Type: List<List<TabLine>>
     */
    private fun findTabSystems(lines: List<String>): List<List<TabLine>> {
        val stringCount = tuning.strings.size
        val runs = mutableListOf<List<TabLine>>()
        var currentRun = mutableListOf<TabLine>()
        for ((lineIndex, line) in lines.withIndex()) {
            val tabLine = parseTabLine(line, lineIndex)
            if (tabLine != null) {
                currentRun += tabLine
            } else if (currentRun.isNotEmpty()) {
                runs += currentRun
                currentRun = mutableListOf()
            }
        }
        if (currentRun.isNotEmpty()) runs += currentRun
        return runs.flatMap { run ->
            when {
                run.size % stringCount == 0 -> run.chunked(stringCount)
                run.size in stringCount + 1..stringCount + EXTRA_STRINGS_ALLOWED -> listOf(run.take(stringCount))
                else -> emptyList()
            }
        }
    }

    /**
     * @param line One normalized line of text. Type: String
     * @param lineIndex The line's position in the text. Type: Int
     * @return The line's string name and tab content, or null if it is not a tab line. Type: TabLine?
     */
    private fun parseTabLine(line: String, lineIndex: Int): TabLine? {
        val lineMatch = TAB_LINE_PATTERN.matchEntire(line) ?: return null
        val content = withoutTextAfterLastBar(lineMatch.groupValues[2])
        val dashCount = content.count { it == '-' }
        val markCount = content.count { !it.isWhitespace() }
        if (dashCount < MINIMUM_DASHES || dashCount * DASH_SHARE_DIVISOR < markCount) return null
        return TabLine(stringName = lineMatch.groupValues[1], content = content, lineIndex = lineIndex)
    }

    /**
     * @param systemLines One line per string, top (highest) string first. Type: List<TabLine>
     * @return The system's notes ordered by column, then low string first. Type: List<TabNote>
     */
    private fun notesInSystem(systemLines: List<TabLine>): List<TabNote> {
        val positionedNotes = systemLines.flatMapIndexed { lineIndex, tabLine ->
            val stringIndex = stringIndexFor(tabLine.stringName, lineIndex)
            FRET_NUMBER_PATTERN.findAll(tabLine.content).map { fretMatch ->
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

    /**
     * One line of a tab system.
     *
     * @property stringName Name written before the line, or empty if none. Type: String
     * @property content The line's tab marks. Type: String
     * @property lineIndex The line's position in the original text. Type: Int
     */
    private data class TabLine(val stringName: String, val content: String, val lineIndex: Int)

    private companion object {
        /** An optional string name, then tab content starting with a bar, colon or dash. */
        val TAB_LINE_PATTERN = Regex("^\\s*([A-Ga-g][#b]?)?\\s*([|:-].*)$")
        val FRET_NUMBER_PATTERN = Regex("\\d+")
        val DASH_LOOKALIKES = Regex("[\u2010-\u2015\u2212\uFE58\uFE63\uFF0D]")
        const val MINIMUM_DASHES = 3
        /** A tab line is at least one third dashes; text lines are not. */
        const val DASH_SHARE_DIVISOR = 3
        const val EXTRA_STRINGS_ALLOWED = 2

        /**
         * Undoes what websites, PDFs and word processors do to tabs: typographic
         * dashes, non-breaking spaces, tab characters and broken bars.
         *
         * @param line Raw line. Type: String
         * @return The line in plain ASCII tab form. Type: String
         */
        fun normalizedLine(line: String): String {
            return line
                .replace(DASH_LOOKALIKES, "-")
                .replace('\u00A0', ' ')
                .replace('\t', ' ')
                .replace('\u00A6', '|')
        }

        /**
         * Drops repeat counts and comments written after the closing bar, such as "x4".
         *
         * @param content Tab content of one line. Type: String
         * @return The content up to and including the last bar, if what follows has no dashes. Type: String
         */
        fun withoutTextAfterLastBar(content: String): String {
            val lastBarIndex = content.lastIndexOf('|')
            if (lastBarIndex < 0) return content
            val trailingText = content.substring(lastBarIndex + 1)
            return if ('-' in trailingText) content else content.substring(0, lastBarIndex + 1)
        }
    }
}
