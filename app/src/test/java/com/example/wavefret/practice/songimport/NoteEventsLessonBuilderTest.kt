package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.music.NoteEvent
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NoteEventsLessonBuilderTest {

    private val builder = NoteEventsLessonBuilder(BassTunings.STANDARD, FretboardPositionFinder(BassTunings.STANDARD))

    private fun event(midi: Int, onsetMillis: Long) = NoteEvent(midi, onsetMillis, durationMillis = 400)

    @Test
    fun buildsALessonWithTitleTempoNotesAndTimings() {
        val lesson = builder.build("Riff", 100, listOf(event(28, 0), event(31, 600), event(33, 1_200)))

        assertEquals("Riff", lesson.title)
        assertEquals(100, lesson.tempoBpm)
        // A1 after G1 stays in the hand position: E string, fret 5
        assertEquals(listOf(TabNote(0, 0), TabNote(0, 3), TabNote(0, 5)), lesson.notes)
        assertEquals(listOf(0L, 600L, 1_200L), lesson.noteOnsetsMillis)
    }

    @Test
    fun eventsAreSortedByOnset() {
        val lesson = builder.build("Riff", 100, listOf(event(33, 500), event(28, 0)))

        assertEquals(listOf(0L, 500L), lesson.noteOnsetsMillis)
    }

    @Test
    fun chordsAreReducedToTheirLowestNote() {
        val lesson = builder.build("Chord", 100, listOf(event(40, 0), event(28, 10), event(45, 20), event(33, 500)))

        assertEquals(listOf(TabNote(0, 0), TabNote(1, 0)), lesson.notes)
    }

    @Test
    fun notesBelowTheBassAreRaisedByOctaves() {
        // B0 (MIDI 23) from a five-string part becomes B1 (35): A string, fret 2
        val lesson = builder.build("Low", 100, listOf(event(23, 0)))

        assertEquals(listOf(TabNote(1, 2)), lesson.notes)
    }

    @Test
    fun notesAboveTheNeckAreLoweredByOctaves() {
        // C#5 (MIDI 73) is above fret 20 of the G string (MIDI 63), so it becomes C#4 (61): G string, fret 18
        val lesson = builder.build("High", 100, listOf(event(73, 0)))

        assertEquals(listOf(TabNote(3, 18)), lesson.notes)
    }

    @Test
    fun noEventsIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { builder.build("Empty", 100, emptyList()) }
    }
}
