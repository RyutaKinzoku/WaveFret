package com.example.wavefret.practice

import com.example.wavefret.common.music.BassTuning

/**
 * Parses WaveFret's own plain-text tab format into an [Exercise]:
 *
 * ```
 * # comment
 * title: Open strings
 * tempo: 80
 * E0 A0 D0 G0
 * ```
 *
 * An optional `timing:` line lists each note's start time in milliseconds,
 * and an optional `source:` line names the recording it came from
 * (`source: youtube <videoId>` or `source: audio <storedFileName>`).
 *
 * Each note token is a string name from the tuning (case-insensitive)
 * immediately followed by a fret number. Note lines are read in order and
 * joined into one sequence. `#` starts a comment that runs to end of line.
 *
 * @property tuning Tuning whose string names the tab refers to. Type: BassTuning
 */
class TabTextParser(private val tuning: BassTuning) {

    /**
     * @param tabText Full text of the tab. Type: String
     * @return The parsed exercise. Type: Exercise
     * @throws TabParseException If any line is malformed or no notes are present.
     */
    fun parse(tabText: String): Exercise {
        var title = UNTITLED_EXERCISE_TITLE
        var tempoBpm = DEFAULT_TEMPO_BPM
        val notes = mutableListOf<TabNote>()
        var noteOnsetsMillis: List<Long>? = null
        var timingLineNumber = 0
        var mediaSource: LessonMediaSource? = null

        tabText.lines().forEachIndexed { lineIndex, rawLine ->
            val lineNumber = lineIndex + 1
            val line = rawLine.substringBefore(COMMENT_MARKER).trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith(TITLE_HEADER, ignoreCase = true) ->
                    title = line.substring(TITLE_HEADER.length).trim()
                line.startsWith(TIMING_HEADER, ignoreCase = true) -> {
                    noteOnsetsMillis = parseTimings(line.substring(TIMING_HEADER.length).trim(), lineNumber)
                    timingLineNumber = lineNumber
                }
                line.startsWith(SOURCE_HEADER, ignoreCase = true) ->
                    mediaSource = parseMediaSource(line.substring(SOURCE_HEADER.length).trim(), lineNumber)
                line.startsWith(TEMPO_HEADER, ignoreCase = true) ->
                    tempoBpm = parseTempo(line.substring(TEMPO_HEADER.length).trim(), lineNumber)
                else -> line.split(WHITESPACE).mapTo(notes) { parseNoteToken(it, lineNumber) }
            }
        }

        if (notes.isEmpty()) throw TabParseException(null, "Tab contains no notes")
        if (noteOnsetsMillis != null && noteOnsetsMillis.size != notes.size) {
            throw TabParseException(timingLineNumber, "Expected ${notes.size} timings, found ${noteOnsetsMillis.size}")
        }
        return try {
            Exercise(
                title = title,
                tuning = tuning,
                tempoBpm = tempoBpm,
                notes = notes,
                noteOnsetsMillis = noteOnsetsMillis,
                mediaSource = mediaSource
            )
        } catch (invalidTiming: IllegalArgumentException) {
            throw TabParseException(timingLineNumber, invalidTiming.message ?: "Invalid timing")
        }
    }

    /**
     * @param tempoText Text after the tempo header. Type: String
     * @param lineNumber Line being parsed, for error reporting. Type: Int
     * @return The tempo in beats per minute. Type: Int
     */
    private fun parseTempo(tempoText: String, lineNumber: Int): Int {
        val tempoBpm = tempoText.toIntOrNull()
        if (tempoBpm == null || tempoBpm <= 0) {
            throw TabParseException(lineNumber, "Tempo must be a positive whole number, was '$tempoText'")
        }
        return tempoBpm
    }

    /**
     * @param sourceText Text after the source header, e.g. "youtube dQw4w9WgXcQ". Type: String
     * @param lineNumber Line being parsed, for error reporting. Type: Int
     * @return The lesson's recording. Type: LessonMediaSource
     */
    private fun parseMediaSource(sourceText: String, lineNumber: Int): LessonMediaSource {
        val sourceKind = sourceText.substringBefore(' ').lowercase()
        val sourceValue = sourceText.substringAfter(' ', missingDelimiterValue = "").trim()
        if (sourceValue.isEmpty()) throw TabParseException(lineNumber, "Source '$sourceText' has no value")
        return when (sourceKind) {
            YOUTUBE_SOURCE -> LessonMediaSource.YouTubeVideo(sourceValue)
            AUDIO_SOURCE -> LessonMediaSource.StoredAudio(sourceValue)
            else -> throw TabParseException(lineNumber, "Unknown source '$sourceKind'")
        }
    }

    /**
     * @param timingText Text after the timing header. Type: String
     * @param lineNumber Line being parsed, for error reporting. Type: Int
     * @return Each note's start time, in milliseconds. Type: List<Long>
     */
    private fun parseTimings(timingText: String, lineNumber: Int): List<Long> {
        return timingText.split(WHITESPACE).filter { it.isNotEmpty() }.map { timingToken ->
            timingToken.toLongOrNull()?.takeIf { it >= 0 }
                ?: throw TabParseException(lineNumber, "'$timingToken' is not a time in milliseconds")
        }
    }

    /**
     * @param noteToken A single token such as "A5". Type: String
     * @param lineNumber Line being parsed, for error reporting. Type: Int
     * @return The note the token describes. Type: TabNote
     */
    private fun parseNoteToken(noteToken: String, lineNumber: Int): TabNote {
        val tokenMatch = NOTE_TOKEN_PATTERN.matchEntire(noteToken)
            ?: throw TabParseException(lineNumber, "'$noteToken' is not a string name followed by a fret")
        val (stringName, fretText) = tokenMatch.destructured
        val stringIndex = tuning.strings.indexOfFirst { it.name.equals(stringName, ignoreCase = true) }
        if (stringIndex < 0) {
            throw TabParseException(lineNumber, "'$stringName' is not a string in the ${tuning.name} tuning")
        }
        return TabNote(stringIndex = stringIndex, fret = fretText.toInt())
    }

    companion object {
        /** Tempo used when the tab has no tempo line. Type: Int */
        const val DEFAULT_TEMPO_BPM = 80

        /** Title used when the tab has no title line. Type: String */
        const val UNTITLED_EXERCISE_TITLE = "Untitled"

        private const val COMMENT_MARKER = "#"
        private const val TITLE_HEADER = "title:"
        private const val TEMPO_HEADER = "tempo:"
        private const val TIMING_HEADER = "timing:"
        private const val SOURCE_HEADER = "source:"

        /** Source kind for YouTube videos. Type: String */
        const val YOUTUBE_SOURCE = "youtube"

        /** Source kind for stored audio files. Type: String */
        const val AUDIO_SOURCE = "audio"
        private val WHITESPACE = Regex("\\s+")
        private val NOTE_TOKEN_PATTERN = Regex("([A-Za-z]+)(\\d+)")
    }
}
