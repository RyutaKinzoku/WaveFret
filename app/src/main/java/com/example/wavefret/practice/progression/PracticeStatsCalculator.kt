package com.example.wavefret.practice.progression

import com.example.wavefret.practice.Exercise

/**
 * Derives the home screen's progress numbers from practice history. Days
 * are counted in the player's local time zone, passed as a UTC offset so
 * the calculation stays deterministic in tests.
 *
 * @property exerciseProgression Decides which exercises are mastered. Type: ExerciseProgression
 */
class PracticeStatsCalculator(private val exerciseProgression: ExerciseProgression) {

    /**
     * @param results Practice history. Type: List<PracticeResult>
     * @param exercises Exercises available. Type: List<Exercise>
     * @param nowMillis Current wall-clock time. Type: Long
     * @param utcOffsetMillis Local time zone's offset from UTC at nowMillis. Type: Int
     * @return The headline numbers. Type: PracticeStats
     */
    fun calculate(
        results: List<PracticeResult>,
        exercises: List<Exercise>,
        nowMillis: Long,
        utcOffsetMillis: Int
    ): PracticeStats {
        val today = localDayOf(nowMillis, utcOffsetMillis)
        val practiceDays = results.map { localDayOf(it.completedAtMillis, utcOffsetMillis) }.toSet()
        return PracticeStats(
            totalRuns = results.size,
            runsInLastSevenDays = results.count { today - localDayOf(it.completedAtMillis, utcOffsetMillis) in 0 until DAYS_PER_WEEK },
            currentStreakDays = streakEndingAt(today, practiceDays),
            masteredExerciseCount = exercises.count { exerciseProgression.isMastered(it.title, results) },
            exerciseCount = exercises.size
        )
    }

    /**
     * @param today Local day number of now. Type: Long
     * @param practiceDays Local day numbers with at least one run. Type: Set<Long>
     * @return Consecutive practice days ending today, or yesterday if today has none yet. Type: Int
     */
    private fun streakEndingAt(today: Long, practiceDays: Set<Long>): Int {
        var streakDay = if (today in practiceDays) today else today - 1
        var streakDays = 0
        while (streakDay in practiceDays) {
            streakDays++
            streakDay--
        }
        return streakDays
    }

    /**
     * @param epochMillis A wall-clock instant. Type: Long
     * @param utcOffsetMillis Local offset from UTC. Type: Int
     * @return Number of the local calendar day containing that instant. Type: Long
     */
    private fun localDayOf(epochMillis: Long, utcOffsetMillis: Int): Long {
        return Math.floorDiv(epochMillis + utcOffsetMillis, MILLIS_PER_DAY)
    }

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
        const val DAYS_PER_WEEK = 7
    }
}
