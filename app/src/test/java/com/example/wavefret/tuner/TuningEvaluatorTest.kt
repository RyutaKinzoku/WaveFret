package com.example.wavefret.tuner

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow

class TuningEvaluatorTest {

    private val evaluator = TuningEvaluator(BassTunings.STANDARD)

    @Test
    fun evaluateReturnsInTuneWhenExactlyOnPitch() {
        val result = evaluator.evaluate(41.20)
        assertEquals("E", result.closestString.name)
        assertEquals(TuningStatus.IN_TUNE, result.status)
    }

    @Test
    fun evaluateReturnsFlatWhenBelowToleranceWindow() {
        val flatFrequency = 41.20 * 2.0.pow(-6.0 / 1200.0)
        val result = evaluator.evaluate(flatFrequency)
        assertEquals("E", result.closestString.name)
        assertEquals(TuningStatus.FLAT, result.status)
    }

    @Test
    fun evaluateReturnsSharpWhenAboveToleranceWindow() {
        val sharpFrequency = 41.20 * 2.0.pow(6.0 / 1200.0)
        val result = evaluator.evaluate(sharpFrequency)
        assertEquals("E", result.closestString.name)
        assertEquals(TuningStatus.SHARP, result.status)
    }

    @Test
    fun evaluateStaysInTuneAtTheEdgeOfTheToleranceWindow() {
        val edgeFrequency = 41.20 * 2.0.pow(4.9 / 1200.0)
        val result = evaluator.evaluate(edgeFrequency)
        assertEquals(TuningStatus.IN_TUNE, result.status)
    }

    @Test
    fun evaluatePicksTheClosestStringWhenFrequencyIsBetweenTwoStrings() {
        // 52 Hz sits between E (41.20) and A (55.00), but is closer to A.
        val result = evaluator.evaluate(52.0)
        assertEquals("A", result.closestString.name)
    }

    @Test
    fun evaluateReportsCentsOffsetRelativeToClosestString() {
        val sharpFrequency = 55.00 * 2.0.pow(10.0 / 1200.0)
        val result = evaluator.evaluate(sharpFrequency)
        assertEquals("A", result.closestString.name)
        assertEquals(10.0, result.centsOffset, 0.01)
    }
}