package com.example.wavefret.practice.progression

import com.example.wavefret.practice.Exercise

/**
 * Turns difficulty ratings and practice history into a learning path:
 * exercises ordered easiest first, which ones are finished or mastered,
 * and what to play next. Finishing a run in any mode marks an exercise
 * as finished, so it is never suggested again (it can still be replayed).
 * Mastery requires playing in time, so only tempo-mode runs count
 * towards it.
 *
 * @property difficultyScorer Rates each exercise. Type: DifficultyScorer
 * @property masteryScorePercent Tempo-mode score needed to master an exercise. Type: Int
 */
class ExerciseProgression(
    private val difficultyScorer: DifficultyScorer,
    private val masteryScorePercent: Int = DEFAULT_MASTERY_SCORE_PERCENT
) {

    /**
     * @param exercises Exercises to order. Type: List<Exercise>
     * @return The exercises with their ratings, easiest first (stable for ties). Type: List<RankedExercise>
     */
    fun orderedByDifficulty(exercises: List<Exercise>): List<RankedExercise> {
        return exercises
            .map { RankedExercise(it, difficultyScorer.score(it)) }
            .sortedBy { it.difficultyScore.overallScore }
    }

    /**
     * @param exerciseTitle Exercise to check. Type: String
     * @param results Practice history. Type: List<PracticeResult>
     * @return True if a tempo-mode run reached the mastery score. Type: Boolean
     */
    fun isMastered(exerciseTitle: String, results: List<PracticeResult>): Boolean {
        return results.any {
            it.exerciseTitle == exerciseTitle && it.practiceMode == PracticeMode.TEMPO &&
                it.scorePercent >= masteryScorePercent
        }
    }

    /**
     * @param exerciseTitle Exercise to check. Type: String
     * @param results Practice history. Type: List<PracticeResult>
     * @return True if a run of the exercise was finished in any mode. Type: Boolean
     */
    fun isFinished(exerciseTitle: String, results: List<PracticeResult>): Boolean {
        return results.any { it.exerciseTitle == exerciseTitle }
    }

    /**
     * @param exercises Exercises available. Type: List<Exercise>
     * @param results Practice history. Type: List<PracticeResult>
     * @return The easiest exercise not yet finished, or null if all are. Type: Exercise?
     */
    fun recommendNext(exercises: List<Exercise>, results: List<PracticeResult>): Exercise? {
        val finishedTitles = results.mapTo(HashSet()) { it.exerciseTitle }
        return orderedByDifficulty(exercises)
            .firstOrNull { it.exercise.title !in finishedTitles }
            ?.exercise
    }

    /**
     * @param exerciseTitle Exercise to check. Type: String
     * @param results Practice history. Type: List<PracticeResult>
     * @return Highest score in any mode, or null if never completed. Type: Int?
     */
    fun bestScorePercent(exerciseTitle: String, results: List<PracticeResult>): Int? {
        return results.filter { it.exerciseTitle == exerciseTitle }.maxOfOrNull { it.scorePercent }
    }

    private companion object {
        const val DEFAULT_MASTERY_SCORE_PERCENT = 90
    }
}
