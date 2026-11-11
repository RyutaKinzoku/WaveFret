package com.example.wavefret.practice

import com.example.wavefret.common.music.BassTunings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LessonEditorTest {

    private val draft = Exercise(
        title = "Draft",
        tuning = BassTunings.STANDARD,
        tempoBpm = 90,
        notes = listOf(TabNote(0, 0), TabNote(1, 2), TabNote(2, 2)),
        noteOnsetsMillis = listOf(0L, 500L, 1_000L)
    )

    @Test
    fun changingANoteKeepsItsTiming() {
        val edited = LessonEditor(draft).changeNote(1, TabNote(0, 7)).lesson

        assertEquals(TabNote(0, 7), edited.notes[1])
        assertEquals(listOf(0L, 500L, 1_000L), edited.noteOnsetsMillis)
    }

    @Test
    fun deletingANoteRemovesItsTimingToo() {
        val edited = LessonEditor(draft).deleteNote(0).lesson

        assertEquals(listOf(TabNote(1, 2), TabNote(2, 2)), edited.notes)
        assertEquals(listOf(500L, 1_000L), edited.noteOnsetsMillis)
    }

    @Test
    fun theLastNoteCannotBeDeleted() {
        val editor = LessonEditor(draft).deleteNote(0).deleteNote(0)

        assertThrows(IllegalStateException::class.java) { editor.deleteNote(0) }
    }

    @Test
    fun insertedNotesGoHalfwayToTheNextNote() {
        val edited = LessonEditor(draft).insertNoteAfter(0, TabNote(0, 3)).lesson

        assertEquals(TabNote(0, 3), edited.notes[1])
        assertEquals(listOf(0L, 250L, 500L, 1_000L), edited.noteOnsetsMillis)
    }

    @Test
    fun noteInsertedAfterTheLastOneIsOneBeatLater() {
        // 90 BPM: one beat = 667 ms
        val edited = LessonEditor(draft).insertNoteAfter(2, TabNote(3, 0)).lesson

        assertEquals(1_667L, edited.noteOnsetsMillis!!.last())
    }

    @Test
    fun lessonsWithoutTimingStayWithoutTiming() {
        val untimed = draft.copy(noteOnsetsMillis = null)

        val edited = LessonEditor(untimed).insertNoteAfter(0, TabNote(0, 3)).deleteNote(2).lesson

        assertEquals(null, edited.noteOnsetsMillis)
        assertEquals(3, edited.noteCount)
    }

    @Test
    fun titleAndTempoCanBeChanged() {
        val edited = LessonEditor(draft).rename("  Billie Jean  ").changeTempo(117).lesson

        assertEquals("Billie Jean", edited.title)
        assertEquals(117, edited.tempoBpm)
    }

    @Test
    fun blankTitleIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { LessonEditor(draft).rename("   ") }
    }
}
