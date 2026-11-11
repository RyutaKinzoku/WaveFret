package com.example.wavefret.common.music

import kotlin.math.pow

/** Number of frets (semitones) that make up one octave. */
private const val FRETS_PER_OCTAVE = 12.0

/**
 * Computes the pitch of a fretted note in 12-tone equal temperament:
 * each fret raises the open string by one semitone.
 *
 * @param openStringFrequencyHz Frequency of the string played open, in Hz. Type: Double
 * @param fret Fret number, where 0 is the open string. Type: Int
 * @return The frequency of the fretted note, in Hz. Type: Double
 */
fun fretFrequencyHz(openStringFrequencyHz: Double, fret: Int): Double {
    require(fret >= 0) { "Fret must not be negative, was $fret" }
    return openStringFrequencyHz * 2.0.pow(fret / FRETS_PER_OCTAVE)
}
