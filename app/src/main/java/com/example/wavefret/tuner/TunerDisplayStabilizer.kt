package com.example.wavefret.tuner

/**
 * Turns the tuner's fast, noisy stream of readings into a display a human
 * can follow while turning a tuning peg. It does two things:
 *
 * - Hold: when the signal drops out (a plucked note decaying, a noisy
 *   frame), the last reading stays on screen for a while instead of
 *   flashing back to "Play a note".
 * - String lock: a different string only replaces the one on screen after
 *   several consecutive readings agree on it, so a single misread frame
 *   can't make the display jump between strings.
 *
 * Readings of the string already on screen always update it immediately,
 * so the needle still follows the peg in real time.
 *
 * @property holdDurationMillis How long the last agreeing reading stays on
 *   screen once readings stop agreeing with it. Type: Long
 * @property stringChangeConfirmationCount Consecutive readings of another
 *   string needed before the display switches to it. Type: Int
 */
class TunerDisplayStabilizer(
    private val holdDurationMillis: Long,
    private val stringChangeConfirmationCount: Int
) {

    init {
        require(holdDurationMillis >= 0) { "holdDurationMillis must not be negative" }
        require(stringChangeConfirmationCount >= 1) { "stringChangeConfirmationCount must be at least 1" }
    }

    private var displayedState: TunerDisplayState = TunerDisplayState.Listening
    private var lastAgreeingReadingMillis = 0L
    private var candidateStringName: String? = null
    private var candidateReadingCount = 0

    /**
     * @param latestState The newest reading, straight from the formatter. Type: TunerDisplayState
     * @param timestampMillis When the reading was taken, on a monotonic clock. Type: Long
     * @return What the screen should show now. Type: TunerDisplayState
     */
    fun stabilize(latestState: TunerDisplayState, timestampMillis: Long): TunerDisplayState {
        if (isHoldExpired(timestampMillis)) {
            displayedState = TunerDisplayState.Listening
        }
        val currentState = displayedState

        when {
            latestState !is TunerDisplayState.Detected -> forgetCandidateString()
            currentState !is TunerDisplayState.Detected -> showReading(latestState, timestampMillis)
            latestState.stringName == currentState.stringName -> showReading(latestState, timestampMillis)
            else -> considerStringChange(latestState, timestampMillis)
        }
        return displayedState
    }

    /**
     * @param timestampMillis Current time, on the same clock as the readings. Type: Long
     * @return True once the displayed reading has outlived its hold. Type: Boolean
     */
    private fun isHoldExpired(timestampMillis: Long): Boolean {
        return timestampMillis - lastAgreeingReadingMillis > holdDurationMillis
    }

    /**
     * Puts a reading on screen and restarts the hold from its timestamp.
     *
     * @param reading Reading to display. Type: TunerDisplayState.Detected
     * @param timestampMillis When the reading was taken. Type: Long
     */
    private fun showReading(reading: TunerDisplayState.Detected, timestampMillis: Long) {
        displayedState = reading
        lastAgreeingReadingMillis = timestampMillis
        forgetCandidateString()
    }

    /**
     * Counts a reading of a string other than the displayed one, and
     * switches to it once enough consecutive readings agree.
     *
     * @param reading Reading of another string. Type: TunerDisplayState.Detected
     * @param timestampMillis When the reading was taken. Type: Long
     */
    private fun considerStringChange(reading: TunerDisplayState.Detected, timestampMillis: Long) {
        if (reading.stringName == candidateStringName) {
            candidateReadingCount++
        } else {
            candidateStringName = reading.stringName
            candidateReadingCount = 1
        }
        if (candidateReadingCount >= stringChangeConfirmationCount) {
            showReading(reading, timestampMillis)
        }
    }

    /**
     * Drops any partly-confirmed string change, so only consecutive
     * readings count toward switching.
     */
    private fun forgetCandidateString() {
        candidateStringName = null
        candidateReadingCount = 0
    }
}