package com.example.wavefret.practice.songimport.scan

/**
 * The clef at the start of a music staff, which fixes the pitch of each
 * line, and how far its notes are lowered to be played on a bass.
 *
 * @property bottomLineStep Natural note on the bottom line, counted in letter steps from C0 (C0 = 0, D0 = 1, …). Type: Int
 * @property semitonesToBass Semitones added to the written pitch to get the pitch played on the bass. Type: Int
 */
enum class Clef(val bottomLineStep: Int, val semitonesToBass: Int) {
    /** Bass clef: bottom line G2; bass parts are written an octave above how they sound. */
    BASS(bottomLineStep = 18, semitonesToBass = -12),

    /** Treble clef: bottom line E4; a melody is moved two octaves down, into the bass's range. */
    TREBLE(bottomLineStep = 30, semitonesToBass = -24),

    /** No clef was recognized; read like a bass clef. */
    UNKNOWN(bottomLineStep = 18, semitonesToBass = -12)
}
