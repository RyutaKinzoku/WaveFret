package com.example.wavefret.common.music

/**
 * A frequency resolved to the nearest musical note.
 *
 * @property noteName Note letter name, with "#" for sharps (e.g. "E", "F#"). Type: String
 * @property octave Scientific pitch octave number (A4 = concert pitch, octave 4). Type: Int
 * @property centsOffset How far the original frequency was from this note's
 *   exact pitch, in cents. Positive is sharp, negative is flat. Type: Double
 */
data class MappedNote(
    val noteName: String,
    val octave: Int,
    val centsOffset: Double
)