package com.example.wavefret.practice.progression

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeStatsCalculatorTest {

    private val calculator = PracticeStatsCalculator(ExerciseProgression(DifficultyScorer(), masteryScorePercent = 90))
    private val exercises = listOf("A", "B", "C").map {
        Exercise(it, BassTunings.STANDARD, 80, listOf(TabNote(0, 0)))
    }

    private fun runOnDay(day: Int, title: String = "A", mode: PracticeMode = PracticeMode.WAIT, score: Int = 50) =
        PracticeResult(title, mode, score, hitCount = 1, missCount = 1, completedAtMillis = day * DAY_MILLIS + HOUR_MILLIS)

    private fun statsAtDay(day: Int, results: List<PracticeResult>) =
        calculator.calculate(results, exercises, nowMillis = day * DAY_MILLIS + 20 * HOUR_MILLIS, utcOffsetMillis = 0)

    @Test
    fun withNoHistoryThePlayerIsNew() {
        val stats = statsAtDay(10, emptyList())

        assertTrue(stats.isNewPlayer)
        assertEquals(0, stats.currentStreakDays)
        assertEquals(0, stats.totalRuns)
    }

    @Test
    fun countsRunsAndMasteredExercises() {
        val results = listOf(runOnDay(1), runOnDay(2, "B", PracticeMode.TEMPO, 95), runOnDay(3, "B", PracticeMode.TEMPO, 92))
        val stats = statsAtDay(3, results)

        assertFalse(stats.isNewPlayer)
        assertEquals(3, stats.totalRuns)
        assertEquals(1, stats.masteredExerciseCount)
        assertEquals(3, stats.exerciseCount)
    }

    @Test
    fun streakCountsConsecutiveDaysEndingToday() {
        val stats = statsAtDay(10, listOf(runOnDay(7), runOnDay(8), runOnDay(9), runOnDay(10), runOnDay(10)))

        assertEquals(4, stats.currentStreakDays)
    }

    @Test
    fun streakIsKeptIfThePlayerPractisedYesterdayButNotYetToday() {
        val stats = statsAtDay(10, listOf(runOnDay(8), runOnDay(9)))

        assertEquals(2, stats.currentStreakDays)
    }

    @Test
    fun streakIsBrokenByAMissedDay() {
        val stats = statsAtDay(10, listOf(runOnDay(5), runOnDay(6), runOnDay(8)))

        assertEquals(0, stats.currentStreakDays)
    }

    @Test
    fun runsThisWeekCountsTheLastSevenDaysIncludingToday() {
        val stats = statsAtDay(10, listOf(runOnDay(3), runOnDay(4), runOnDay(9), runOnDay(10)))

        assertEquals(3, stats.runsInLastSevenDays)
    }

    @Test
    fun daysFollowTheLocalTimeZone() {
        // 23:00 UTC on day 9 is already day 10 at UTC+2
        val lateResult = PracticeResult("A", PracticeMode.WAIT, 50, 1, 1, completedAtMillis = 9 * DAY_MILLIS + 23 * HOUR_MILLIS)
        val stats = calculator.calculate(
            listOf(lateResult), exercises, nowMillis = 10 * DAY_MILLIS + 12 * HOUR_MILLIS, utcOffsetMillis = (2 * HOUR_MILLIS).toInt()
        )

        assertEquals(1, stats.currentStreakDays)
    }

    private companion object {
        const val HOUR_MILLIS = 3_600_000L
        const val DAY_MILLIS = 24 * HOUR_MILLIS
    }
}
