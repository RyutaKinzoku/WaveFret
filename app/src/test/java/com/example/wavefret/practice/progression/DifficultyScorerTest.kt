package com.example.wavefret.practice.progression

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DifficultyScorerTest {

    private val difficultyScorer = DifficultyScorer()

    private fun exercise(tempoBpm: Int, vararg notes: TabNote) =
        Exercise("Test", BassTunings.STANDARD, tempoBpm, notes.toList())

    @Test
    fun repeatedOpenStringAtSlowTempoScoresLow() {
        val score = difficultyScorer.score(exercise(60, TabNote(0, 0), TabNote(0, 0), TabNote(0, 0)))

        assertTrue("was ${score.overallScore}", score.overallScore < 15)
    }

    @Test
    fun scoreStaysWithinZeroToOneHundred() {
        val hardest = exercise(240, TabNote(0, 0), TabNote(3, 20), TabNote(0, 1), TabNote(3, 19))

        assertTrue(difficultyScorer.score(hardest).overallScore in 0..100)
    }

    @Test
    fun fasterTempoIsHarder() {
        val notes = arrayOf(TabNote(0, 3), TabNote(1, 5), TabNote(2, 2))

        assertTrue(difficultyScorer.score(exercise(140, *notes)).overallScore > difficultyScorer.score(exercise(70, *notes)).overallScore)
    }

    @Test
    fun countsHandPositionShiftsLargerThanTheFretSpan() {
        val score = difficultyScorer.score(exercise(80, TabNote(0, 1), TabNote(0, 3), TabNote(0, 9), TabNote(0, 2)))

        // 1 -> 3 stays within a four-fret hand span; 3 -> 9 and 9 -> 2 are shifts
        assertEquals(2, score.positionShiftCount)
    }

    @Test
    fun openStringsNeverCountAsPositionShifts() {
        val score = difficultyScorer.score(exercise(80, TabNote(0, 7), TabNote(1, 0), TabNote(1, 7)))

        assertEquals(0, score.positionShiftCount)
    }

    @Test
    fun stringCrossingsAreWeightedByHowManyStringsAreSkipped() {
        val score = difficultyScorer.score(exercise(80, TabNote(0, 0), TabNote(1, 0), TabNote(3, 0), TabNote(3, 2)))

        assertEquals(3, score.stringCrossingDistance)
    }

    @Test
    fun shiftsAndCrossingsMakeAnExerciseHarderAtTheSameTempo() {
        val easy = exercise(80, TabNote(1, 0), TabNote(1, 2), TabNote(1, 3), TabNote(1, 2))
        val hard = exercise(80, TabNote(0, 1), TabNote(3, 12), TabNote(0, 3), TabNote(3, 14))

        assertTrue(difficultyScorer.score(hard).overallScore > difficultyScorer.score(easy).overallScore)
    }

    @Test
    fun labelsFollowTheScore() {
        assertEquals(DifficultyLevel.BEGINNER, DifficultyLevel.forScore(10))
        assertEquals(DifficultyLevel.INTERMEDIATE, DifficultyLevel.forScore(45))
        assertEquals(DifficultyLevel.ADVANCED, DifficultyLevel.forScore(80))
    }
}
