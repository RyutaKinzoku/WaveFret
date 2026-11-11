package com.example.wavefret.practice.progression

/**
 * Encouraging verdict on a completed run, chosen from its score.
 *
 * @property lowestScorePercent Lowest score that earns this rating. Type: Int
 */
enum class PerformanceRating(val lowestScorePercent: Int) {
    KEEP_PRACTICING(0),
    GOOD(50),
    GREAT(80),
    PERFECT(100);

    companion object {
        /**
         * @param scorePercent Score of the run, 0..100. Type: Int
         * @return The rating that score earns. Type: PerformanceRating
         */
        fun forScore(scorePercent: Int): PerformanceRating = entries.last { scorePercent >= it.lowestScorePercent }
    }
}
