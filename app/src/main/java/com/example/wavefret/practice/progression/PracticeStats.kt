package com.example.wavefret.practice.progression

/**
 * Headline numbers for the home screen's progress card.
 *
 * @property totalRuns Completed practice runs ever. Type: Int
 * @property runsInLastSevenDays Completed runs in the last seven local days, today included. Type: Int
 * @property currentStreakDays Consecutive local days with practice, ending today or yesterday. Type: Int
 * @property masteredExerciseCount Exercises mastered in tempo mode. Type: Int
 * @property exerciseCount Exercises available. Type: Int
 */
data class PracticeStats(
    val totalRuns: Int,
    val runsInLastSevenDays: Int,
    val currentStreakDays: Int,
    val masteredExerciseCount: Int,
    val exerciseCount: Int
) {
    /** True before the first completed run, so the home screen can onboard. Type: Boolean */
    val isNewPlayer: Boolean get() = totalRuns == 0
}
