package com.example.wavefret.practice.progression

import com.example.wavefret.practice.Exercise

/**
 * Builds the end-of-run summary (the peak moment of a practice session)
 * from the finished run and the history before it.
 *
 * @property exerciseProgression Supplies mastery, best scores and recommendations. Type: ExerciseProgression
 */
class PracticeSummaryBuilder(private val exerciseProgression: ExerciseProgression) {

    /**
     * @param finishedResult The run that just finished. Type: PracticeResult
     * @param earlierResults History before this run. Type: List<PracticeResult>
     * @param exercises Exercises available. Type: List<Exercise>
     * @return The summary to show. Type: PracticeSummary
     */
    fun build(finishedResult: PracticeResult, earlierResults: List<PracticeResult>, exercises: List<Exercise>): PracticeSummary {
        val title = finishedResult.exerciseTitle
        val previousBest = exerciseProgression.bestScorePercent(title, earlierResults)
        val resultsIncludingThisRun = earlierResults + finishedResult
        val otherExercises = exercises.filterNot { it.title == title }
        return PracticeSummary(
            result = finishedResult,
            rating = PerformanceRating.forScore(finishedResult.scorePercent),
            isNewBest = previousBest == null || finishedResult.scorePercent > previousBest,
            isJustMastered = !exerciseProgression.isMastered(title, earlierResults) &&
                exerciseProgression.isMastered(title, resultsIncludingThisRun),
            nextExercise = exerciseProgression.recommendNext(otherExercises, resultsIncludingThisRun)
        )
    }
}
