package com.example.wavefret.common.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FretFrequencyTest {

    @Test
    fun openStringReturnsTheOpenStringFrequency() {
        assertEquals(41.20, fretFrequencyHz(openStringFrequencyHz = 41.20, fret = 0), 1e-9)
    }

    @Test
    fun twelfthFretIsExactlyOneOctaveUp() {
        assertEquals(110.0, fretFrequencyHz(openStringFrequencyHz = 55.0, fret = 12), 1e-9)
    }

    @Test
    fun secondFretOfDStringIsE2() {
        assertEquals(82.41, fretFrequencyHz(openStringFrequencyHz = 73.42, fret = 2), 0.01)
    }

    @Test
    fun negativeFretIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            fretFrequencyHz(openStringFrequencyHz = 41.20, fret = -1)
        }
    }
}
