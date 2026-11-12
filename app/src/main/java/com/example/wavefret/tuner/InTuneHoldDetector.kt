package com.example.wavefret.tuner

import kotlin.math.abs

/**
 * Decides when a string is really in tune: the needle must stay within the
 * tolerance for a short while, not just pass through it, and brief
 * dropouts while the string rings don't restart the wait. Confirms once,
 * then needs a [reset] (or leaving the tolerance) to confirm again.
 *
 * @property toleranceCents How close counts as in tune. Type: Double
 * @property holdMillis How long it must stay in tune. Type: Long
 * @property gapToleranceMillis Dropouts up to this long are ignored. Type: Long
 */
class InTuneHoldDetector(
    private val toleranceCents: Double = DEFAULT_TOLERANCE_CENTS,
    private val holdMillis: Long = DEFAULT_HOLD_MILLIS,
    private val gapToleranceMillis: Long = DEFAULT_GAP_TOLERANCE_MILLIS
) {

    private var inTuneSinceMillis: Long? = null
    private var lastInTuneMillis = 0L
    private var hasConfirmed = false

    /**
     * @param cents Needle position in cents, or null when nothing is heard. Type: Double?
     * @param timeMillis When the reading was taken. Type: Long
     * @return True exactly when the string has just been confirmed in tune. Type: Boolean
     */
    fun onReading(cents: Double?, timeMillis: Long): Boolean {
        when {
            cents == null -> if (timeMillis - lastInTuneMillis > gapToleranceMillis) reset()
            abs(cents) > toleranceCents -> reset()
            else -> {
                if (inTuneSinceMillis == null) inTuneSinceMillis = timeMillis
                lastInTuneMillis = timeMillis
            }
        }
        val inTuneSince = inTuneSinceMillis ?: return false
        if (hasConfirmed || timeMillis - inTuneSince < holdMillis) return false
        hasConfirmed = true
        return true
    }

    /**
     * @return Unit
     */
    fun reset() {
        inTuneSinceMillis = null
        hasConfirmed = false
    }

    /** Default in-tune tolerance in cents. Type: Double */
    companion object {
        /** How close counts as in tune: ±5 cents, about what the ear can tell on a bass. */
        const val DEFAULT_TOLERANCE_CENTS = 5.0
        private const val DEFAULT_HOLD_MILLIS = 700L
        private const val DEFAULT_GAP_TOLERANCE_MILLIS = 200L
    }
}
