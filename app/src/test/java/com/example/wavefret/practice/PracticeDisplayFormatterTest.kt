package com.example.wavefret.practice

import com.example.wavefret.common.music.BassTunings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PracticeDisplayFormatterTest {

    private val exercise = Exercise(
        title = "Three notes",
        tuning = BassTunings.STANDARD,
        tempoBpm = 80,
        notes = listOf(TabNote(0, 3), TabNote(1, 5), TabNote(3, 2))
    )
    private val practiceDisplayFormatter = PracticeDisplayFormatter()

    private fun progress(
        currentNoteIndex: Int,
        hitCount: Int = currentNoteIndex,
        missedNoteIndices: Set<Int> = emptySet(),
        lastReading: PitchReading = PitchReading.SILENT
    ) = PracticeProgress(
        currentNoteIndex = currentNoteIndex,
        noteCount = exercise.noteCount,
        hitCount = hitCount,
        missCount = missedNoteIndices.size,
        lastReading = lastReading,
        missedNoteIndices = missedNoteIndices
    )

    @Test
    fun showsTheStringAndFretOfTheCurrentNote() {
        val displayState = practiceDisplayFormatter.format(exercise, progress(currentNoteIndex = 1))

        assertEquals("A", displayState.currentStringName)
        assertEquals(5, displayState.currentFret)
    }

    @Test
    fun showsOneBasedPositionOutOfTotal() {
        val displayState = practiceDisplayFormatter.format(exercise, progress(currentNoteIndex = 1))

        assertEquals(2, displayState.currentNoteNumber)
        assertEquals(3, displayState.noteCount)
    }

    @Test
    fun silentReadingMeansListening() {
        val displayState = practiceDisplayFormatter.format(exercise, progress(0, lastReading = PitchReading.SILENT))

        assertEquals(PracticeFeedback.LISTENING, displayState.feedback)
    }

    @Test
    fun matchingReadingMeansCorrect() {
        val displayState = practiceDisplayFormatter.format(exercise, progress(1, lastReading = PitchReading.MATCHING))

        assertEquals(PracticeFeedback.CORRECT, displayState.feedback)
    }

    @Test
    fun otherNoteMeansWrongNote() {
        val displayState = practiceDisplayFormatter.format(exercise, progress(0, lastReading = PitchReading.NOT_MATCHING))

        assertEquals(PracticeFeedback.WRONG_NOTE, displayState.feedback)
    }

    @Test
    fun completedExerciseShowsCompleteWithNoCurrentNote() {
        val displayState = practiceDisplayFormatter.format(exercise, progress(currentNoteIndex = 3))

        assertEquals(PracticeFeedback.COMPLETE, displayState.feedback)
        assertNull(displayState.currentStringName)
        assertNull(displayState.currentFret)
        assertEquals(3, displayState.currentNoteNumber)
    }

    @Test
    fun scoreIsThePercentageOfNotesHitOnceComplete() {
        val displayState = practiceDisplayFormatter.format(
            exercise, progress(currentNoteIndex = 3, hitCount = 2, missedNoteIndices = setOf(1))
        )

        assertEquals(67, displayState.scorePercent)
    }

    @Test
    fun scoreIsHiddenUntilComplete() {
        val displayState = practiceDisplayFormatter.format(exercise, progress(currentNoteIndex = 1))

        assertNull(displayState.scorePercent)
    }

    @Test
    fun noteStatusesReflectHitMissedCurrentAndUpcoming() {
        val displayState = practiceDisplayFormatter.format(
            exercise, progress(currentNoteIndex = 2, hitCount = 1, missedNoteIndices = setOf(0))
        )

        assertEquals(
            listOf(TabNoteStatus.MISSED, TabNoteStatus.HIT, TabNoteStatus.CURRENT),
            displayState.noteStatuses
        )
    }
}
