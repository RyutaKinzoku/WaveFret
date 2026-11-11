package com.example.wavefret.practice

/**
 * Wait mode: shows a note and does not move on until it is played
 * correctly. There is no timing, so nothing can be missed.
 *
 * @property exercise The exercise being practiced. Type: Exercise
 * @param noteMatcher Decides whether a pitch is the expected note. Type: NoteMatcher
 * @param hitConfirmer Turns matching readings into discrete hits. Type: HitConfirmer
 */
class WaitModePracticeSession(
    override val exercise: Exercise,
    noteMatcher: NoteMatcher,
    hitConfirmer: HitConfirmer
) : PracticeSession {

    private val expectedNoteJudge = ExpectedNoteJudge(exercise, noteMatcher, hitConfirmer)
    private var currentNoteIndex = 0
    private var lastReading = PitchReading.SILENT

    /**
     * @param frequencyHz Pitch detected in the latest buffer, or null for none. Type: Double?
     * @param timestampMillis Unused: wait mode ignores timing. Type: Long
     * @return Progress after taking this reading into account. Type: PracticeProgress
     */
    override fun onPitchDetected(frequencyHz: Double?, timestampMillis: Long): PracticeProgress {
        if (currentNoteIndex >= exercise.noteCount) return currentProgress()
        lastReading = expectedNoteJudge.classify(currentNoteIndex, frequencyHz)
        if (expectedNoteJudge.confirmsHit(lastReading)) {
            expectedNoteJudge.onAdvancedPast(currentNoteIndex)
            currentNoteIndex++
        }
        return currentProgress()
    }

    /** @return Progress without consuming a new reading. Type: PracticeProgress */
    override fun currentProgress(): PracticeProgress = PracticeProgress(
        currentNoteIndex = currentNoteIndex,
        noteCount = exercise.noteCount,
        hitCount = currentNoteIndex,
        missCount = 0,
        lastReading = lastReading
    )

    /** @return Progress after going back to the first note. Type: PracticeProgress */
    override fun restart(): PracticeProgress {
        currentNoteIndex = 0
        lastReading = PitchReading.SILENT
        expectedNoteJudge.reset()
        return currentProgress()
    }
}
