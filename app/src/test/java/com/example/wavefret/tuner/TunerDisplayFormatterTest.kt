package com.example.wavefret.tuner

import org.junit.Assert.assertEquals
import org.junit.Test

class TunerDisplayFormatterTest {

    private val displayFormatter = TunerDisplayFormatter()
    private val eString = BassTunings.STANDARD.strings.first { it.name == "E" }
    private val aString = BassTunings.STANDARD.strings.first { it.name == "A" }

    @Test
    fun formatsNullResultAsListening() {
        val displayState = displayFormatter.format(null)

        assertEquals(TunerDisplayState.Listening, displayState)
    }

    @Test
    fun formatsInTuneResultWithZeroCentsAndNoRotation() {
        val tuningResult = TuningResult(eString, 0.0, TuningStatus.IN_TUNE)

        val displayState = displayFormatter.format(tuningResult)

        assertEquals(
            TunerDisplayState.Detected("E", "0¢", TuningStatus.IN_TUNE, 0f),
            displayState
        )
    }

    @Test
    fun formatsSharpResultWithPositiveSignAndRoundedCents() {
        val tuningResult = TuningResult(aString, 7.6, TuningStatus.SHARP)

        val displayState = displayFormatter.format(tuningResult) as TunerDisplayState.Detected

        assertEquals("+8¢", displayState.centsOffsetLabel)
    }

    @Test
    fun formatsFlatResultWithNegativeSign() {
        val tuningResult = TuningResult(aString, -12.4, TuningStatus.FLAT)

        val displayState = displayFormatter.format(tuningResult) as TunerDisplayState.Detected

        assertEquals("-12¢", displayState.centsOffsetLabel)
    }

    @Test
    fun calculatesProportionalNeedleRotationWithinDisplayRange() {
        val tuningResult = TuningResult(aString, -25.0, TuningStatus.FLAT)

        val displayState = displayFormatter.format(tuningResult) as TunerDisplayState.Detected

        assertEquals(-22.5f, displayState.needleRotationDegrees, 0.01f)
    }

    @Test
    fun clampsNeedleRotationForCentsBeyondDisplayRange() {
        val tuningResult = TuningResult(aString, 73.0, TuningStatus.SHARP)

        val displayState = displayFormatter.format(tuningResult) as TunerDisplayState.Detected

        assertEquals(45.0f, displayState.needleRotationDegrees, 0.01f)
        assertEquals("+73¢", displayState.centsOffsetLabel)
    }
}