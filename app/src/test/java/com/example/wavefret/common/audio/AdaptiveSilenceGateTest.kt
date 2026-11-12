package com.example.wavefret.common.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveSilenceGateTest {

    private fun noise(amplitude: Double, seed: Long = 7L) = SyntheticSignals.whiteNoise(2048, amplitude, seed)
    private fun softNote(amplitude: Double) =
        SyntheticSignals.sine(55.0, 44_100, 2048).map { (it * amplitude).toFloat() }.toFloatArray()

    @Test
    fun aSoftlyPluckedNoteInAQuietRoomGetsThrough() {
        val gate = AdaptiveSilenceGate()
        repeat(30) { gate.hasSignal(noise(0.001)) }

        // RMS ~0.0035: the old fixed gate (0.01) threw this away
        assertTrue(gate.hasSignal(softNote(0.005)))
    }

    @Test
    fun steadyBackgroundNoiseIsLearnedAndIgnored() {
        val gate = AdaptiveSilenceGate()

        repeat(200) { gate.hasSignal(noise(0.02, seed = it.toLong())) }

        assertFalse(gate.hasSignal(noise(0.02, seed = 999)))
    }

    @Test
    fun aNoteClearlyAboveTheBackgroundGetsThroughEvenInANoisyRoom() {
        val gate = AdaptiveSilenceGate()
        repeat(200) { gate.hasSignal(noise(0.02, seed = it.toLong())) }

        assertTrue(gate.hasSignal(softNote(0.1)))
    }

    @Test
    fun aLongSustainedNoteIsNotMistakenForBackgroundNoise() {
        val gate = AdaptiveSilenceGate()
        repeat(30) { gate.hasSignal(noise(0.001)) }

        // ~10 seconds of one note at ~40 readings per second
        val stillHeard = (1..400).map { gate.hasSignal(softNote(0.05)) }

        assertTrue(stillHeard.last())
    }

    @Test
    fun digitalSilenceIsSilent() {
        assertFalse(AdaptiveSilenceGate().hasSignal(FloatArray(2048)))
    }
}
