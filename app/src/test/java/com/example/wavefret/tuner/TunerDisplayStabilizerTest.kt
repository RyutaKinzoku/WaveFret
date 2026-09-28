package com.example.wavefret.tuner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TunerDisplayStabilizerTest {

    private val stabilizer = TunerDisplayStabilizer(
        holdDurationMillis = HOLD_DURATION_MILLIS,
        stringChangeConfirmationCount = CONFIRMATION_COUNT
    )

    /**
     * Builds a detected reading. The tuning status is irrelevant to
     * stabilizing, so any value works.
     *
     * @param stringName Name of the string the reading is closest to. Type: String
     * @param needleRotationDegrees Needle angle of the reading. Type: Float
     * @return A detected display state. Type: TunerDisplayState.Detected
     */
    private fun detectedOn(stringName: String, needleRotationDegrees: Float = 0f): TunerDisplayState.Detected {
        return TunerDisplayState.Detected(
            stringName = stringName,
            centsOffsetLabel = "0¢",
            status = TuningStatus.entries.first(),
            needleRotationDegrees = needleRotationDegrees
        )
    }

    @Test
    fun staysListeningWhileNothingIsDetected() {
        assertEquals(TunerDisplayState.Listening, stabilizer.stabilize(TunerDisplayState.Listening, 0L))
    }

    @Test
    fun showsFirstDetectionImmediately() {
        val lowE = detectedOn("E")
        assertEquals(lowE, stabilizer.stabilize(lowE, 0L))
    }

    @Test
    fun keepsShowingLastDetectionDuringHoldAfterSignalDrops() {
        val lowE = detectedOn("E")
        stabilizer.stabilize(lowE, 0L)

        assertEquals(lowE, stabilizer.stabilize(TunerDisplayState.Listening, HOLD_DURATION_MILLIS))
    }

    @Test
    fun returnsToListeningOnceHoldExpires() {
        stabilizer.stabilize(detectedOn("E"), 0L)

        val shown = stabilizer.stabilize(TunerDisplayState.Listening, HOLD_DURATION_MILLIS + 1)

        assertEquals(TunerDisplayState.Listening, shown)
    }

    @Test
    fun sameStringReadingMovesNeedleAndRestartsHold() {
        stabilizer.stabilize(detectedOn("E", needleRotationDegrees = -10f), 0L)
        val retuned = detectedOn("E", needleRotationDegrees = 2f)

        assertEquals(retuned, stabilizer.stabilize(retuned, 3000L))
        assertEquals(retuned, stabilizer.stabilize(TunerDisplayState.Listening, 3000L + HOLD_DURATION_MILLIS))
    }

    @Test
    fun ignoresSingleStrayReadingOfAnotherString() {
        val lowE = detectedOn("E")
        stabilizer.stabilize(lowE, 0L)

        assertEquals(lowE, stabilizer.stabilize(detectedOn("D"), 200L))
    }

    @Test
    fun switchesStringAfterEnoughConsecutiveReadings() {
        stabilizer.stabilize(detectedOn("E"), 0L)
        val aString = detectedOn("A")

        stabilizer.stabilize(aString, 200L)
        stabilizer.stabilize(aString, 400L)
        val shown = stabilizer.stabilize(aString, 600L)

        assertEquals(aString, shown)
    }

    @Test
    fun interruptedReadingsOfAnotherStringDoNotSwitch() {
        val lowE = detectedOn("E")
        stabilizer.stabilize(lowE, 0L)
        val dString = detectedOn("D")

        stabilizer.stabilize(dString, 200L)
        stabilizer.stabilize(dString, 400L)
        stabilizer.stabilize(lowE, 600L)
        stabilizer.stabilize(dString, 800L)
        val shown = stabilizer.stabilize(dString, 1000L)

        assertEquals(lowE, shown)
    }

    @Test
    fun alternatingStrayReadingsDoNotSwitch() {
        val lowE = detectedOn("E")
        stabilizer.stabilize(lowE, 0L)

        stabilizer.stabilize(detectedOn("D"), 200L)
        stabilizer.stabilize(detectedOn("G"), 400L)
        val shown = stabilizer.stabilize(detectedOn("D"), 600L)

        assertEquals(lowE, shown)
    }

    @Test
    fun strayReadingsDoNotExtendHold() {
        stabilizer.stabilize(detectedOn("E"), 0L)
        stabilizer.stabilize(detectedOn("D"), 3000L)

        val shown = stabilizer.stabilize(TunerDisplayState.Listening, HOLD_DURATION_MILLIS + 1)

        assertEquals(TunerDisplayState.Listening, shown)
    }

    @Test
    fun showsNewStringImmediatelyOnceHoldHasExpired() {
        stabilizer.stabilize(detectedOn("E"), 0L)
        val dString = detectedOn("D")

        assertEquals(dString, stabilizer.stabilize(dString, HOLD_DURATION_MILLIS + 1))
    }

    @Test
    fun constructorRejectsNegativeHoldDuration() {
        assertThrows(IllegalArgumentException::class.java) {
            TunerDisplayStabilizer(holdDurationMillis = -1L, stringChangeConfirmationCount = CONFIRMATION_COUNT)
        }
    }

    @Test
    fun constructorRejectsConfirmationCountBelowOne() {
        assertThrows(IllegalArgumentException::class.java) {
            TunerDisplayStabilizer(holdDurationMillis = HOLD_DURATION_MILLIS, stringChangeConfirmationCount = 0)
        }
    }

    companion object {
        private const val HOLD_DURATION_MILLIS = 4000L
        private const val CONFIRMATION_COUNT = 3
    }
}