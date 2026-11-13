package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.practice.TabNote
import com.example.wavefret.practice.songimport.FretboardPositionFinder

/**
 * Places pitches read from sheet music on the fretboard, keeping the hand
 * near where it already is, so they can be shown and edited as tab.
 *
 * @property fretboardPositionFinder Chooses a string and fret for each pitch. Type: FretboardPositionFinder
 */
class NotationTabArranger(private val fretboardPositionFinder: FretboardPositionFinder) {

    /**
     * @param notationSystems Sounding MIDI numbers of each staff, in order. Type: List<List<Int>>
     * @return The same systems as tab columns; pitches the bass can't play are left out. Type: List<List<List<TabNote>>>
     */
    fun arrange(notationSystems: List<List<Int>>): List<List<List<TabNote>>> {
        var previousNote: TabNote? = null
        val playable = fretboardPositionFinder.openStringMidiNumbers().let { openStrings ->
            openStrings.min()..openStrings.max() + fretboardPositionFinder.highestFret
        }
        return notationSystems.map { pitches ->
            pitches.filter { it in playable }.map { midiNumber ->
                fretboardPositionFinder.positionFor(midiNumber, previousNote).also { previousNote = it }.let(::listOf)
            }
        }
    }
}
