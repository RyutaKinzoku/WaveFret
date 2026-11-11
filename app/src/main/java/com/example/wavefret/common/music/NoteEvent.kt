package com.example.wavefret.common.music

/**
 * One note heard or read from a source, before it is placed on a fretboard.
 *
 * @property midiNumber Pitch as a MIDI note number (A4 = 69). Type: Int
 * @property onsetMillis When the note starts, from the start of the source. Type: Long
 * @property durationMillis How long it sounds. Type: Long
 */
data class NoteEvent(
    val midiNumber: Int,
    val onsetMillis: Long,
    val durationMillis: Long
)
