package com.example.wavefret.practice

import com.example.wavefret.common.music.centsBetween
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Answers the practice engine's core yes/no question: is the detected pitch
 * the note the tab expects? This is a comparison against a known target,
 * not open-ended transcription.
 *
 * @property toleranceCents How far off, in cents, still counts as the right note. Type: Double
 * @property acceptOctaveErrors When true, the right note in a different octave
 *   also matches, absorbing bass harmonic/octave misdetections. Type: Boolean
 */
class NoteMatcher(
    private val toleranceCents: Double = DEFAULT_TOLERANCE_CENTS,
    private val acceptOctaveErrors: Boolean = false
) {

    /**
     * @param expectedFrequencyHz Pitch the tab asks for, in Hz. Type: Double
     * @param detectedFrequencyHz Pitch that was heard, in Hz. Type: Double
     * @return True if the detected pitch counts as the expected note. Type: Boolean
     */
    fun matches(expectedFrequencyHz: Double, detectedFrequencyHz: Double): Boolean {
        val centsOff = centsBetween(expectedFrequencyHz, detectedFrequencyHz)
        val comparedCents = if (acceptOctaveErrors) centsFromNearestOctave(centsOff) else centsOff
        return abs(comparedCents) <= toleranceCents
    }

    /**
     * @param cents Signed distance from the expected note, in cents. Type: Double
     * @return The same distance after removing whole octaves. Type: Double
     */
    private fun centsFromNearestOctave(cents: Double): Double {
        val wholeOctaves = (cents / CENTS_PER_OCTAVE).roundToInt()
        return cents - wholeOctaves * CENTS_PER_OCTAVE
    }

    private companion object {
        const val DEFAULT_TOLERANCE_CENTS = 40.0
        const val CENTS_PER_OCTAVE = 1200.0
    }
}
