package com.example.wavefret.tuner

import org.junit.Assert.assertEquals
import org.junit.Test

class InTuneHoldDetectorTest {

    private fun detector() = InTuneHoldDetector(toleranceCents = 5.0, holdMillis = 600, gapToleranceMillis = 200)

    /** Feeds one reading every 25 ms and returns the times at which a tuned string was confirmed. */
    private fun confirmationTimes(detector: InTuneHoldDetector, readings: List<Double?>): List<Long> =
        readings.mapIndexedNotNull { index, cents -> (index * 25L).takeIf { detector.onReading(cents, it) } }

    @Test
    fun holdingInTuneLongEnoughConfirmsOnce() {
        val confirmations = confirmationTimes(detector(), List(60) { 2.0 })

        assertEquals(listOf(600L), confirmations)
    }

    @Test
    fun passingThroughInTuneDoesNotConfirm() {
        val sweep = (-30..30 step 2).map { it.toDouble() }

        assertEquals(emptyList<Long>(), confirmationTimes(detector(), sweep))
    }

    @Test
    fun driftingOutRestartsTheHold() {
        val readings = List(20) { 1.0 } + listOf(12.0) + List(30) { 1.0 }

        assertEquals(listOf(525L + 600L), confirmationTimes(detector(), readings))
    }

    @Test
    fun aShortDropoutDoesNotRestartTheHold() {
        val readings = List(12) { 1.0 } + listOf<Double?>(null, null) + List(20) { 1.0 }

        assertEquals(listOf(600L), confirmationTimes(detector(), readings))
    }

    @Test
    fun resetForgetsTheHold() {
        val detector = detector()
        repeat(20) { detector.onReading(1.0, it * 25L) }

        detector.reset()

        assertEquals(false, detector.onReading(1.0, 500))
    }
}
