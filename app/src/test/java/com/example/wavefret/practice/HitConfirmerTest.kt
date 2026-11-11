package com.example.wavefret.practice

import org.junit.Assert.assertEquals
import org.junit.Test

class HitConfirmerTest {

    @Test
    fun singleMatchingReadingIsNotYetAHit() {
        val hitConfirmer = HitConfirmer(requiredConsecutiveMatches = 2)

        assertEquals(false, hitConfirmer.onReading(PitchReading.MATCHING))
    }

    @Test
    fun consecutiveMatchingReadingsConfirmAHit() {
        val hitConfirmer = HitConfirmer(requiredConsecutiveMatches = 2)

        val confirmations = listOf(PitchReading.MATCHING, PitchReading.MATCHING).map(hitConfirmer::onReading)

        assertEquals(listOf(false, true), confirmations)
    }

    @Test
    fun interruptedMatchingRunStartsCountingAgain() {
        val hitConfirmer = HitConfirmer(requiredConsecutiveMatches = 2)

        val confirmations = listOf(
            PitchReading.MATCHING, PitchReading.NOT_MATCHING, PitchReading.MATCHING, PitchReading.MATCHING
        ).map(hitConfirmer::onReading)

        assertEquals(listOf(false, false, false, true), confirmations)
    }

    @Test
    fun sustainedNoteAfterAHitDoesNotCountAsASecondHit() {
        val hitConfirmer = HitConfirmer(requiredConsecutiveMatches = 1)

        val confirmations = listOf(PitchReading.MATCHING, PitchReading.MATCHING, PitchReading.MATCHING)
            .map(hitConfirmer::onReading)

        assertEquals(listOf(true, false, false), confirmations)
    }

    @Test
    fun silenceAfterAHitReArmsForTheNextHit() {
        val hitConfirmer = HitConfirmer(requiredConsecutiveMatches = 1)

        val confirmations = listOf(PitchReading.MATCHING, PitchReading.SILENT, PitchReading.MATCHING)
            .map(hitConfirmer::onReading)

        assertEquals(listOf(true, false, true), confirmations)
    }

    @Test
    fun differentNoteAfterAHitReArmsForTheNextHit() {
        val hitConfirmer = HitConfirmer(requiredConsecutiveMatches = 1)

        val confirmations = listOf(PitchReading.MATCHING, PitchReading.NOT_MATCHING, PitchReading.MATCHING)
            .map(hitConfirmer::onReading)

        assertEquals(listOf(true, false, true), confirmations)
    }

    @Test
    fun resetForgetsAPartialRun() {
        val hitConfirmer = HitConfirmer(requiredConsecutiveMatches = 2)
        hitConfirmer.onReading(PitchReading.MATCHING)

        hitConfirmer.reset()

        assertEquals(false, hitConfirmer.onReading(PitchReading.MATCHING))
    }
}
