package com.example.wavefret.practice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteMatcherTest {

    private val noteMatcher = NoteMatcher(toleranceCents = 40.0)

    @Test
    fun exactFrequencyMatches() {
        assertTrue(noteMatcher.matches(expectedFrequencyHz = 55.0, detectedFrequencyHz = 55.0))
    }

    @Test
    fun slightlyOutOfTuneFrequencyStillMatches() {
        // ~31 cents sharp of A1
        assertTrue(noteMatcher.matches(expectedFrequencyHz = 55.0, detectedFrequencyHz = 56.0))
    }

    @Test
    fun neighbouringSemitoneDoesNotMatch() {
        // A#1 is 100 cents above A1
        assertFalse(noteMatcher.matches(expectedFrequencyHz = 55.0, detectedFrequencyHz = 58.27))
    }

    @Test
    fun octaveAboveDoesNotMatchByDefault() {
        assertFalse(noteMatcher.matches(expectedFrequencyHz = 41.2, detectedFrequencyHz = 82.4))
    }

    @Test
    fun octaveAboveMatchesWhenOctaveErrorsAreAccepted() {
        val lenientNoteMatcher = NoteMatcher(toleranceCents = 40.0, acceptOctaveErrors = true)

        assertTrue(lenientNoteMatcher.matches(expectedFrequencyHz = 41.2, detectedFrequencyHz = 82.4))
    }

    @Test
    fun octaveBelowMatchesWhenOctaveErrorsAreAccepted() {
        val lenientNoteMatcher = NoteMatcher(toleranceCents = 40.0, acceptOctaveErrors = true)

        assertTrue(lenientNoteMatcher.matches(expectedFrequencyHz = 98.0, detectedFrequencyHz = 49.0))
    }

    @Test
    fun wrongNoteDoesNotMatchEvenWhenOctaveErrorsAreAccepted() {
        val lenientNoteMatcher = NoteMatcher(toleranceCents = 40.0, acceptOctaveErrors = true)

        assertFalse(lenientNoteMatcher.matches(expectedFrequencyHz = 41.2, detectedFrequencyHz = 61.7))
    }
}
