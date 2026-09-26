package com.example.wavefret.common.music

import kotlin.math.ln

/**
 * @param referenceHz The reference frequency being compared against. Type: Double
 * @param frequencyHz The frequency to measure. Type: Double
 * @return How far frequencyHz is from referenceHz, in cents. Positive is
 *   sharp (higher), negative is flat (lower). Type: Double
 */
fun centsBetween(referenceHz: Double, frequencyHz: Double): Double {
    return 1200.0 * ln(frequencyHz / referenceHz) / ln(2.0)
}