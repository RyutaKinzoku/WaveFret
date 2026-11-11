package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTuning
import com.example.wavefret.common.music.centsBetween
import com.example.wavefret.practice.TabNote
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Chooses where on the fretboard to play a pitch that came without tab
 * (e.g. MusicXML written as standard notation). It keeps the hand close
 * to the previous note when possible and otherwise prefers the lowest
 * fret, the way a bassist would sight-read a line.
 *
 * @property tuning Tuning whose strings are available. Type: BassTuning
 * @property highestFret Highest fret a note may be placed on. Type: Int
 */
class FretboardPositionFinder(
    private val tuning: BassTuning,
    val highestFret: Int = DEFAULT_HIGHEST_FRET
) {

    /**
     * @return MIDI note number of each open string, low to high. Type: List<Int>
     */
    fun openStringMidiNumbers(): List<Int> = tuning.strings.map { bassString ->
        A4_MIDI_NUMBER + (centsBetween(A4_FREQUENCY_HZ, bassString.frequencyHz) / CENTS_PER_SEMITONE).roundToInt()
    }

    /**
     * @param midiNoteNumber Pitch to place, as a MIDI note number. Type: Int
     * @param previousNote Position of the note before it, or null for the first note. Type: TabNote?
     * @return The chosen string and fret. Type: TabNote
     * @throws IllegalArgumentException If the pitch can't be played in this tuning.
     */
    fun positionFor(midiNoteNumber: Int, previousNote: TabNote?): TabNote {
        val playablePositions = openStringMidiNumbers().mapIndexedNotNull { stringIndex, openStringMidiNumber ->
            val fret = midiNoteNumber - openStringMidiNumber
            if (fret in 0..highestFret) TabNote(stringIndex, fret) else null
        }
        require(playablePositions.isNotEmpty()) {
            "MIDI note $midiNoteNumber can't be played in the ${tuning.name} tuning up to fret $highestFret"
        }
        val referenceFret = previousNote?.fret ?: 0
        return playablePositions.minWith(compareBy({ abs(it.fret - referenceFret) }, { it.fret }))
    }

    private companion object {
        const val DEFAULT_HIGHEST_FRET = 20
        const val A4_MIDI_NUMBER = 69
        const val A4_FREQUENCY_HZ = 440.0
        const val CENTS_PER_SEMITONE = 100.0
    }
}
