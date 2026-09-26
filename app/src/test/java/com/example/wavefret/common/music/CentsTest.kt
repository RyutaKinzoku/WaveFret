package com.example.wavefret.common.music

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow

class CentsTest {

    @Test
    fun centsBetweenReturnsZeroForIdenticalFrequencies() {
        assertEquals(0.0, centsBetween(referenceHz = 110.0, frequencyHz = 110.0), 0.0001)
    }

    @Test
    fun centsBetweenReturnsOneHundredForAFullSemitoneUp() {
        val oneSemitoneUp = 110.0 * 2.0.pow(1.0 / 12.0)
        assertEquals(100.0, centsBetween(referenceHz = 110.0, frequencyHz = oneSemitoneUp), 0.0001)
    }

    @Test
    fun centsBetweenReturnsNegativeForAFrequencyBelowReference() {
        val slightlyFlat = 110.0 * 2.0.pow(-0.2 / 1200.0)
        assertEquals(-0.2, centsBetween(referenceHz = 110.0, frequencyHz = slightlyFlat), 0.0001)
    }

    @Test
    fun centsBetweenReturnsOneThousandTwoHundredForAFullOctaveUp() {
        assertEquals(1200.0, centsBetween(referenceHz = 110.0, frequencyHz = 220.0), 0.0001)
    }
}