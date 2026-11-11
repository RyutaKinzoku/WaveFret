package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FretboardPositionFinderTest {

    private val fretboardPositionFinder = FretboardPositionFinder(BassTunings.STANDARD, highestFret = 20)

    @Test
    fun lowestNoteIsTheOpenLowString() {
        assertEquals(TabNote(0, 0), fretboardPositionFinder.positionFor(midiNoteNumber = E1, previousNote = null))
    }

    @Test
    fun withoutAPreviousNoteTheLowestFretIsChosen() {
        assertEquals(TabNote(1, 0), fretboardPositionFinder.positionFor(midiNoteNumber = A1, previousNote = null))
    }

    @Test
    fun withAPreviousNoteThePositionClosestToItIsChosen() {
        val position = fretboardPositionFinder.positionFor(midiNoteNumber = D2, previousNote = TabNote(0, 5))

        assertEquals(TabNote(1, 5), position)
    }

    @Test
    fun noteBelowTheLowestStringIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            fretboardPositionFinder.positionFor(midiNoteNumber = E1 - 1, previousNote = null)
        }
    }

    @Test
    fun noteAboveTheHighestFretIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            fretboardPositionFinder.positionFor(midiNoteNumber = G2 + 21, previousNote = null)
        }
    }

    @Test
    fun midiNumberOfOpenStringsMatchesTheirFrequency() {
        assertEquals(G2, fretboardPositionFinder.openStringMidiNumbers().last())
    }

    private companion object {
        const val E1 = 28
        const val A1 = 33
        const val D2 = 38
        const val G2 = 43
    }
}
