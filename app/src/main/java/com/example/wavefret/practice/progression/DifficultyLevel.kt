package com.example.wavefret.practice.progression

/**
 * Coarse difficulty band shown next to an exercise.
 *
 * @property lowestScore Lowest overall score that falls in this band. Type: Int
 */
enum class DifficultyLevel(val lowestScore: Int) {
    BEGINNER(0),
    INTERMEDIATE(30),
    ADVANCED(65);

    companion object {
        /**
         * @param overallScore Overall difficulty score, 0..100. Type: Int
         * @return The band that score falls in. Type: DifficultyLevel
         */
        fun forScore(overallScore: Int): DifficultyLevel = entries.last { overallScore >= it.lowestScore }
    }
}
