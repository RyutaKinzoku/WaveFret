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

    /** @return Progress without consuming a new reading. Type: PracticeProgress */
    fun currentProgress(): PracticeProgress

    /** @return Progress after going back to the start of the exercise. Type: PracticeProgress */
    fun restart(): PracticeProgress
}
