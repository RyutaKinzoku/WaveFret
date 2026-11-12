package com.example.wavefret.tuner

import kotlin.math.roundToInt

/**
 * Turns a needle position in cents into what the tuner screen draws: a
 * signed, rounded cents label and the needle angle (the scale shows ±50
 * cents over ±45°; beyond that the needle stops at the end of the scale).
 */
class TunerDisplayFormatter {

    /**
     * @param cents Needle position; positive is sharp. Type: Double
     * @return Label such as "+8¢", "0¢" or "-12¢". Type: String
     */
    fun centsLabel(cents: Double): String {
        val roundedCents = cents.roundToInt()
        return if (roundedCents > 0) "+$roundedCents¢" else "$roundedCents¢"
    }

    /**
     * @param cents Needle position; positive is sharp. Type: Double
     * @return Needle angle in degrees, 0 being upright. Type: Float
     */
    fun needleRotationDegrees(cents: Double): Float {
        val clampedCents = cents.coerceIn(-MAX_DISPLAYED_CENTS, MAX_DISPLAYED_CENTS)
        return (clampedCents / MAX_DISPLAYED_CENTS * MAX_NEEDLE_ROTATION_DEGREES).toFloat()
    }

    private companion object {
        const val MAX_DISPLAYED_CENTS = 50.0
        const val MAX_NEEDLE_ROTATION_DEGREES = 45.0
    }
}
