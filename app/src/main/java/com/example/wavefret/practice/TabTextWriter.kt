package com.example.wavefret.practice

/**
 * Writes an [Exercise] in WaveFret's own plain-text tab format, the exact
 * inverse of [TabTextParser], so imported songs can be stored as text.
 *
 * @property notesPerLine How many note tokens to put on each line. Type: Int
 */
class TabTextWriter(private val notesPerLine: Int = DEFAULT_NOTES_PER_LINE) {

    init {
        require(notesPerLine > 0) { "Each line must hold at least one note" }
    }

    /**
     * @param exercise Exercise to write. Type: Exercise
     * @return The exercise as tab text, ending with a newline. Type: String
     */
    fun write(exercise: Exercise): String = buildString {
        append("title: ").append(exercise.title).append('\n')
        append("tempo: ").append(exercise.tempoBpm).append('\n')
        exercise.notes.chunked(notesPerLine).forEach { lineNotes ->
            append(lineNotes.joinToString(" ") { it.stringName(exercise.tuning) + it.fret }).append('\n')
        }
    }

    private companion object {
        const val DEFAULT_NOTES_PER_LINE = 8
    }
}
