package com.example.wavefret.practice

/**
 * A run through one exercise. Implementations decide when the expected note
 * advances — waiting for the player, or following a tempo — so the
 * audio pipeline and UI can drive either mode the same way.
 */
interface PracticeSession {

    /** The exercise being practiced. Type: Exercise */
    val exercise: Exercise

    /**
     * @param frequencyHz Pitch detected in the latest buffer, or null for none. Type: Double?
     * @param timestampMillis Monotonic time the buffer was captured at. Type: Long
     * @return Progress after taking this reading into account. Type: PracticeProgress
     */
    fun onPitchDetected(frequencyHz: Double?, timestampMillis: Long): PracticeProgress

    /**
     * The pitch the session is waiting for, so the pipeline can listen for
     * it specifically and hear softly played notes.
     *
     * @return Frequency of the current note in Hz, or null once complete. Type: Double?
     */
    fun expectedFrequencyHz(): Double? = exercise.notes.getOrNull(currentProgress().currentNoteIndex)
        ?.expectedFrequencyHz(exercise.tuning)

    /** @return Progress without consuming a new reading. Type: PracticeProgress */
    fun currentProgress(): PracticeProgress

    /** @return Progress after going back to the start of the exercise. Type: PracticeProgress */
    fun restart(): PracticeProgress
}
