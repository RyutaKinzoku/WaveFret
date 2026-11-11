package com.example.wavefret.common.transcription

import org.junit.Assert.assertEquals
import org.junit.Test

class TempoEstimatorTest {

    private val tempoEstimator = TempoEstimator()

    @Test
    fun evenQuarterNotesGiveTheirTempo() {
        assertEquals(100, tempoEstimator.estimateBpm(listOf(0L, 600L, 1_200L, 1_800L, 2_400L)))
    }

    @Test
    fun eighthNotesAreFoldedIntoAPlayableRange() {
        // 250 ms apart is 240 BPM as quarters; folded down to 120
        assertEquals(120, tempoEstimator.estimateBpm((0 until 9).map { it * 250L }))
    }

    @Test
    fun slowNotesAreFoldedUp() {
        // 2 s apart is 30 BPM; folded up to 60
        assertEquals(60, tempoEstimator.estimateBpm(listOf(0L, 2_000L, 4_000L)))
    }

    @Test
    fun theTypicalGapWinsOverOddOnes() {
        assertEquals(100, tempoEstimator.estimateBpm(listOf(0L, 600L, 1_200L, 1_250L, 1_850L, 2_450L, 3_050L)))
    }

    @Test
    fun tooFewNotesFallBackToTheDefault() {
        assertEquals(TempoEstimator.DEFAULT_TEMPO_BPM, tempoEstimator.estimateBpm(listOf(0L)))
    }
}
