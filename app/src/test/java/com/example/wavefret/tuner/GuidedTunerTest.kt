package com.example.wavefret.tuner

import com.example.wavefret.common.music.BassTunings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedTunerTest {

    private val tuning = BassTunings.STANDARD
    private fun guidedTuner() = GuidedTuner(tuning)
    private var clockMillis = 0L

    /** Plays a frequency for a while, one reading every 25 ms, and returns the last state. */
    private fun GuidedTuner.hear(frequencyHz: Double?, durationMillis: Long): List<GuidedTunerState> =
        (0 until durationMillis / 25).map { onReading(frequencyHz, clockMillis).also { clockMillis += 25 } }

    @Test
    fun startsByTuningTheLowEStringAutomatically() {
        val state = guidedTuner().currentState()

        assertEquals(0, state.targetStringIndex)
        assertTrue(state.isAutomatic)
        assertNull(state.needleCents)
    }

    @Test
    fun aFlatEStringShowsFlat() {
        val state = guidedTuner().hear(40.0, 300).last()

        assertEquals(TuningStatus.FLAT, state.status)
        assertTrue(state.needleCents!! < -40)
    }

    @Test
    fun holdingTheEStringInTuneMarksItTunedOnceAndMovesToA() {
        val states = guidedTuner().hear(41.2, 1_200)

        assertEquals(1, states.count { it.justTunedStringIndex == 0 })
        assertEquals(setOf(0), states.last().tunedStringIndices)
        assertEquals(1, states.last().targetStringIndex)
    }

    @Test
    fun anAStringPlayedWhileTuningTheGStringNeverCountsAsTheG() {
        val tuner = guidedTuner()
        tuner.select(3)

        val states = tuner.hear(55.0, 2_000)

        assertTrue(states.none { it.justTunedStringIndex != null })
        assertEquals(3, states.last().targetStringIndex)
    }

    @Test
    fun theEStringsOctaveHarmonicStillTunesTheEString() {
        val states = guidedTuner().hear(82.4, 1_200)

        assertEquals(setOf(0), states.last().tunedStringIndices)
    }

    @Test
    fun tuningEveryStringInTurnFinishes() {
        val tuner = guidedTuner()
        tuning.strings.forEach { bassString -> tuner.hear(bassString.frequencyHz, 1_000); tuner.hear(null, 800) }

        val state = tuner.currentState()
        assertTrue(state.isAllTuned)
        assertNull(state.targetStringIndex)
    }

    @Test
    fun choosingATunedStringUnmarksIt() {
        val tuner = guidedTuner()
        tuner.hear(41.2, 1_200)

        tuner.select(0)

        assertEquals(emptySet<Int>(), tuner.currentState().tunedStringIndices)
        assertFalse(tuner.currentState().isAutomatic)
    }

    @Test
    fun resetUnmarksEverything() {
        val tuner = guidedTuner()
        tuner.hear(41.2, 1_200)

        tuner.resetAll()

        assertEquals(emptySet<Int>(), tuner.currentState().tunedStringIndices)
        assertEquals(0, tuner.currentState().targetStringIndex)
    }

    @Test
    fun needleRestsAfterTheStringStopsRinging() {
        val tuner = guidedTuner()
        tuner.hear(40.5, 300)

        assertNull(tuner.hear(null, 1_000).last().needleCents)
    }
}
