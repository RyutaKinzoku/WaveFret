package com.example.wavefret.common.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AttackDetectorTest {

    private fun attacksFor(vararg levels: Double): List<Boolean> {
        val detector = AttackDetector()
        return levels.map(detector::onLevel)
    }

    @Test
    fun theFirstSoundAfterSilenceIsAnAttack() {
        assertEquals(listOf(false, false, true), attacksFor(0.0, 0.0, 0.05))
    }

    @Test
    fun aSustainedNoteIsNotAttackedAgain() {
        assertEquals(listOf(true, false, false, false), attacksFor(0.05, 0.05, 0.048, 0.05))
    }

    @Test
    fun aFadingNoteIsNotAnAttack() {
        assertEquals(listOf(true, false, false, false, false), attacksFor(0.08, 0.05, 0.03, 0.02, 0.012))
    }

    @Test
    fun fretttingAStillRingingStringIsNotAnAttack() {
        // The level wobbles a little when a finger presses a ringing string
        assertEquals(listOf(true, false, false, false), attacksFor(0.06, 0.03, 0.04, 0.035))
    }

    @Test
    fun repluckingAFadingStringIsAnAttack() {
        assertEquals(listOf(true, false, false, true), attacksFor(0.08, 0.03, 0.012, 0.07))
    }

    @Test
    fun aSoftPluckInAQuietRoomIsAnAttack() {
        assertEquals(listOf(false, false, true), attacksFor(0.0006, 0.0007, 0.0035))
    }

    @Test
    fun roomNoiseFlickerIsNotAnAttack() {
        assertEquals(listOf(false, false, false, false), attacksFor(0.0005, 0.0012, 0.0006, 0.0015))
    }
}
