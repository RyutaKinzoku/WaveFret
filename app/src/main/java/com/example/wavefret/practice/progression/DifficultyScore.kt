package com.example.wavefret.practice.progression

/**
 * How hard an exercise is, overall and by the factors that drive it.
 *
 * @property overallScore Combined difficulty, 0 (trivial) to 100 (hardest). Type: Int
 * @property positionShiftCount Times the fretting hand must move to a new position. Type: Int
 * @property stringCrossingDistance Total strings crossed between consecutive notes. Type: Int
 */
data class DifficultyScore(
    val overallScore: Int,
    val positionShiftCount: Int,
    val stringCrossingDistance: Int
) {
    /** Coarse band the overall score falls in. Type: DifficultyLevel */
    val level: DifficultyLevel get() = DifficultyLevel.forScore(overallScore)
}
