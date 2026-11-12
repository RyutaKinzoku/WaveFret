package com.example.wavefret.tuner

import kotlin.math.abs
import kotlin.math.exp

/**
 * Makes the tuner needle follow the string in real time without shaking:
 * small wobbles are smoothed over a short time constant, a big change (a
 * new pluck, turning the peg a lot) moves the needle straight there, and
 * brief gaps keep the needle in place before it finally rests.
 *
 * @property timeConstantMillis How quickly the needle catches up with small changes. Type: Long
 * @property jumpResetCents Changes bigger than this move the needle immediately. Type: Double
 * @property holdMillis How long the needle stays put without a reading. Type: Long
 */
class NeedleSmoother(
    private val timeConstantMillis: Long = DEFAULT_TIME_CONSTANT_MILLIS,
    private val jumpResetCents: Double = DEFAULT_JUMP_RESET_CENTS,
    private val holdMillis: Long = DEFAULT_HOLD_MILLIS
) {

    private var smoothedCents: Double? = null
    private var lastReadingMillis = 0L

    /**
     * @param cents Latest reading in cents, or null for no pitch. Type: Double?
     * @param timeMillis When it was taken. Type: Long
     * @return Where the needle should point, or null to rest it. Type: Double?
     */
    fun onReading(cents: Double?, timeMillis: Long): Double? {
        val previousCents = smoothedCents
        if (cents == null) {
            if (previousCents != null && timeMillis - lastReadingMillis > holdMillis) smoothedCents = null
            return smoothedCents
        }
        smoothedCents = if (previousCents == null || abs(cents - previousCents) > jumpResetCents) {
            cents
        } else {
            val blend = 1 - exp(-(timeMillis - lastReadingMillis).toDouble() / timeConstantMillis)
            previousCents + blend * (cents - previousCents)
        }
        lastReadingMillis = timeMillis
        return smoothedCents
    }

    /**
     * Forgets the needle position, e.g. when moving to another string.
     *
     * @return Unit
     */
    fun reset() {
        smoothedCents = null
    }

    private companion object {
        const val DEFAULT_TIME_CONSTANT_MILLIS = 90L
        const val DEFAULT_JUMP_RESET_CENTS = 60.0
        const val DEFAULT_HOLD_MILLIS = 500L
    }
}
