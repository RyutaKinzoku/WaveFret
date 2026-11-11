package com.example.wavefret.practice

import com.example.wavefret.common.music.BassTunings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TabNoteTest {

    private val standardTuning = BassTunings.STANDARD

    @Test
    fun openLowEStringResolvesToLowEFrequency() {
        val openLowE = TabNote(stringIndex = 0, fret = 0)

        assertEquals(41.20, openLowE.expectedFrequencyHz(standardTuning), 1e-9)
    }

    @Test
    fun fifthFretOfAStringResolvesToOpenDFrequency() {
        val fifthFretOnA = TabNote(stringIndex = 1, fret = 5)

        assertEquals(73.42, fifthFretOnA.expectedFrequencyHz(standardTuning), 0.01)
    }

    @Test
    fun stringNameComesFromTheTuning() {
        val thirdFretOnG = TabNote(stringIndex = 3, fret = 3)

        assertEquals("G", thirdFretOnG.stringName(standardTuning))
    }

    @Test
    fun negativeStringIndexIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { TabNote(stringIndex = -1, fret = 0) }
    }

    @Test
    fun negativeFretIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { TabNote(stringIndex = 0, fret = -2) }
    }

    @Test
    fun stringIndexOutsideTheTuningIsRejectedWhenResolving() {
        val fifthStringNote = TabNote(stringIndex = 4, fret = 0)

        assertThrows(IllegalArgumentException::class.java) {
            fifthStringNote.expectedFrequencyHz(standardTuning)
        }
    }
}
