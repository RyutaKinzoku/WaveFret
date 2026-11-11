package com.example.wavefret.practice

/**
 * Wait mode: shows a note and does not move on until it is played
 * correctly. There is no timing, so nothing can be missed.
 *
 * @property exercise The exercise being practiced. Type: Exercise
 * @property noteMatcher Decides whether a pitch is the expected note. Type: NoteMatcher
 * @property hitConfirmer Turns matching readings into discrete hits. Type: HitConfirmer
 */
class WaitModePracticeSession(
    override val exercise: Exercise,
    private val noteMatcher: NoteMatcher,
    private val hitConfirmer: HitConfirmer
) : PracticeSession {

    private var currentNoteIndex = 0
    private var lastReading = PitchReading.SILENT

    /**
     * @param frequencyHz Pitch detected in the latest buffer, or null for none. Type: Double?
     * @param timestampMillis Unused: wait mode ignores timing. Type: Long
     * @return Progress after taking this reading into account. Type: PracticeProgress
     */
    override fun onPitchDetected(frequencyHz: Double?, timestampMillis: Long): PracticeProgress {
        if (currentNoteIndex >= exercise.noteCount) return currentProgress()
        val expectedFrequencyHz = exercise.notes[currentNoteIndex].expectedFrequencyHz(exercise.tuning)
        lastReading = classifyReading(expectedFrequencyHz, frequencyHz)
        if (hitConfirmer.onReading(lastReading)) advanceToNextNote(expectedFrequencyHz)
        return currentProgress()
    }

    /**
     * Moves to the next note. The confirmer only stays disarmed when the next
     * note is the same pitch, so a sustained note can't count twice while a
     * legato move to a different note is still heard immediately.
     *
     * @param playedFrequencyHz Pitch of the note that was just hit, in Hz. Type: Double
     * @return Unit
     */
    private fun advanceToNextNote(playedFrequencyHz: Double) {
        currentNoteIndex++
        val nextNote = exercise.notes.getOrNull(currentNoteIndex) ?: return
        val nextFrequencyHz = nextNote.expectedFrequencyHz(exercise.tuning)
        if (!noteMatcher.matches(playedFrequencyHz, nextFrequencyHz)) hitConfirmer.reset()
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
        hitConfirmer.reset()
        return currentProgress()
    }

    /**
     * @param expectedFrequencyHz Pitch of the note now expected, in Hz. Type: Double
     * @param detectedFrequencyHz Pitch heard, or null for none. Type: Double?
     * @return How the reading compares to the expected note. Type: PitchReading
     */
    private fun classifyReading(expectedFrequencyHz: Double, detectedFrequencyHz: Double?): PitchReading = when {
        detectedFrequencyHz == null -> PitchReading.SILENT
        noteMatcher.matches(expectedFrequencyHz, detectedFrequencyHz) -> PitchReading.MATCHING
        else -> PitchReading.NOT_MATCHING
    }
}
