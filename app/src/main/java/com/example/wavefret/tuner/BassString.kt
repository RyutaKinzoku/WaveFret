package com.example.wavefret.tuner

/**
 * A single string on a bass, at its standard (correctly tuned) pitch.
 *
 * @property name Display name for the string (e.g. "E", "A"). Type: String
 * @property frequencyHz The string's correctly-tuned frequency, in Hz. Type: Double
 */
data class BassString(
    val name: String,
    val frequencyHz: Double
)