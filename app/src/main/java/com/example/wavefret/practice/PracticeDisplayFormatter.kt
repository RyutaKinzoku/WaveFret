package com.example.wavefret.practice

import kotlin.math.roundToInt

/** Turns practice progress into what the practice screen should display. */
class PracticeDisplayFormatter {

    /**
     * @param exercise The exercise being practiced. Type: Exercise
     * @param progress Latest progress through it. Type: PracticeProgress
     * @return What the screen should show. Type: PracticeDisplayState
     */
    fun format(exercise: Exercise, progress: PracticeProgress): PracticeDisplayState {
        val currentNote = exercise.notes.getOrNull(progress.currentNoteIndex)
        return PracticeDisplayState(
            currentStringName = currentNote?.stringName(exercise.tuning),
            currentFret = currentNote?.fret,
            currentNoteNumber = minOf(progress.currentNoteIndex + 1, progress.noteCount),
            noteCount = progress.noteCount,
            feedback = feedbackFor(progress),
            scorePercent = if (progress.isComplete) scorePercentOf(progress) else null,
            noteStatuses = List(exercise.noteCount) { noteIndex -> statusOf(noteIndex, progress) },
            playheadPosition = progress.playheadPosition
        )
    }

    /**
     * @param progress Latest progress. Type: PracticeProgress
     * @return Feedback for the most recent reading. Type: PracticeFeedback
     */
    private fun feedbackFor(progress: PracticeProgress): PracticeFeedback = when {
        progress.isComplete -> PracticeFeedback.COMPLETE
        progress.lastReading == PitchReading.MATCHING -> PracticeFeedback.CORRECT
        progress.lastReading == PitchReading.NOT_MATCHING -> PracticeFeedback.WRONG_NOTE
        else -> PracticeFeedback.LISTENING
    }

    /**
     * @param progress Completed progress. Type: PracticeProgress
     * @return Notes hit as a rounded percentage of all notes. Type: Int
     */
    private fun scorePercentOf(progress: PracticeProgress): Int {
        return (progress.hitCount * PERCENT / progress.noteCount.toDouble()).roundToInt()
    }

    /**
     * @param noteIndex Note to classify. Type: Int
     * @param progress Latest progress. Type: PracticeProgress
     * @return How that note should be drawn. Type: TabNoteStatus
     */
    private fun statusOf(noteIndex: Int, progress: PracticeProgress): TabNoteStatus = when {
        noteIndex in progress.missedNoteIndices -> TabNoteStatus.MISSED
        noteIndex < progress.currentNoteIndex -> TabNoteStatus.HIT
        noteIndex == progress.currentNoteIndex -> TabNoteStatus.CURRENT
        else -> TabNoteStatus.UPCOMING
    }

    private companion object {
        const val PERCENT = 100.0
    }
}
