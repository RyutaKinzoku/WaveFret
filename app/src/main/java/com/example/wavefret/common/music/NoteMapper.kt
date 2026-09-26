package com.example.wavefret.common.music

import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Maps a frequency to its nearest note in 12-tone equal temperament,
 * referenced against A4 = 440 Hz, and reports how far off pitch it was.
 */
class NoteMapper {

    /**
     * @param frequencyHz Frequency to map, in Hz. Must be a positive, valid
     *   pitch — callers should only pass a frequency PitchDetector actually
     *   detected, never a null/no-pitch result. Type: Double
     * @return The nearest note and how many cents sharp or flat it was. Type: MappedNote
     */
    fun mapFrequencyToNote(frequencyHz: Double): MappedNote {
        val semitonesFromA4 = centsBetween(REFERENCE_FREQUENCY_HZ, frequencyHz) / 100.0
        val roundedSemitones = semitonesFromA4.roundToInt()
        val centsOffset = (semitonesFromA4 - roundedSemitones) * 100.0

        val midiNumber = REFERENCE_MIDI_NUMBER + roundedSemitones
        val noteIndex = Math.floorMod(midiNumber, NOTE_NAMES.size)
        val octave = Math.floorDiv(midiNumber, NOTE_NAMES.size) - 1

        return MappedNote(NOTE_NAMES[noteIndex], octave, centsOffset)
    }

    companion object {
        private const val REFERENCE_FREQUENCY_HZ = 440.0
        private const val REFERENCE_MIDI_NUMBER = 69
        private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    }
}