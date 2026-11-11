package com.example.wavefret.practice

import kotlin.math.roundToLong

/**
 * Edits a lesson, typically one produced by audio transcription, without
 * breaking its invariants: every note keeps a timing (if the lesson is
 * timed), inserted notes are timed sensibly, and a lesson always keeps at
 * least one note and a title. Each edit returns a new editor.
 *
 * @property lesson The lesson as edited so far. Type: Exercise
 */
class LessonEditor(val lesson: Exercise) {

    /**
     * @param noteIndex Note to change. Type: Int
     * @param newNote Where it should be played instead. Type: TabNote
     * @return An editor with the change applied. Type: LessonEditor
     */
    fun changeNote(noteIndex: Int, newNote: TabNote): LessonEditor =
        LessonEditor(lesson.copy(notes = lesson.notes.toMutableList().also { it[noteIndex] = newNote }))

    /**
     * @param noteIndex Note to remove. Type: Int
     * @return An editor without that note. Type: LessonEditor
     * @throws IllegalStateException If it is the only note left.
     */
    fun deleteNote(noteIndex: Int): LessonEditor {
        check(lesson.noteCount > 1) { "A lesson needs at least one note" }
        return LessonEditor(
            lesson.copy(
                notes = lesson.notes.filterIndexed { index, _ -> index != noteIndex },
                noteOnsetsMillis = lesson.noteOnsetsMillis?.filterIndexed { index, _ -> index != noteIndex }
            )
        )
    }

    /**
     * @param noteIndex Note after which to insert. Type: Int
     * @param newNote Note to insert. Type: TabNote
     * @return An editor with the new note, timed halfway to the next note (or a beat later at the end). Type: LessonEditor
     */
    fun insertNoteAfter(noteIndex: Int, newNote: TabNote): LessonEditor {
        val noteOnsetsMillis = lesson.noteOnsetsMillis
        val newOnsetsMillis = noteOnsetsMillis?.let { onsets ->
            val previousOnset = onsets[noteIndex]
            val newOnset = onsets.getOrNull(noteIndex + 1)?.let { nextOnset -> (previousOnset + nextOnset) / 2 }
                ?: (previousOnset + (MILLIS_PER_MINUTE / lesson.tempoBpm).roundToLong())
            onsets.toMutableList().also { it.add(noteIndex + 1, newOnset) }
        }
        return LessonEditor(
            lesson.copy(
                notes = lesson.notes.toMutableList().also { it.add(noteIndex + 1, newNote) },
                noteOnsetsMillis = newOnsetsMillis
            )
        )
    }

    /**
     * @param newTitle New title; surrounding spaces are removed. Type: String
     * @return An editor with the new title. Type: LessonEditor
     * @throws IllegalArgumentException If the title is blank.
     */
    fun rename(newTitle: String): LessonEditor {
        require(newTitle.isNotBlank()) { "A lesson needs a title" }
        return LessonEditor(lesson.copy(title = newTitle.trim()))
    }

    /**
     * @param newTempoBpm New tempo for tempo mode. Type: Int
     * @return An editor with the new tempo. Type: LessonEditor
     */
    fun changeTempo(newTempoBpm: Int): LessonEditor = LessonEditor(lesson.copy(tempoBpm = newTempoBpm))

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000.0
    }
}
