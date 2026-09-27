package com.example.wavefret.tuner

import com.example.wavefret.common.audio.SyntheticSignals
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TunerControllerTest {

    private val standardTuning = BassTunings.STANDARD

    @Test
    fun emitsNullWhenNoSignalPresent() = runTest {
        val silentBuffer = SyntheticSignals.silence(SAMPLE_COUNT)
        val tunerController = TunerController(
            audioSource = FakePcmAudioSource(listOf(silentBuffer)),
            silenceGate = SilenceGate(),
            pitchDetector = FakePitchDetector(emptyList()),
            pitchSmoother = PitchSmoother(),
            tuningEvaluator = TuningEvaluator(standardTuning)
        )

        val emittedResults = tunerController.tuningResultStream().toList()

        assertEquals(listOf(null), emittedResults)
    }

    @Test
    fun emitsNullWhenPitchDetectorFindsNoPitch() = runTest {
        val audibleBuffer = SyntheticSignals.sine(110.0, SAMPLE_RATE, SAMPLE_COUNT)
        val tunerController = TunerController(
            audioSource = FakePcmAudioSource(listOf(audibleBuffer)),
            silenceGate = SilenceGate(),
            pitchDetector = FakePitchDetector(listOf(null)),
            pitchSmoother = PitchSmoother(),
            tuningEvaluator = TuningEvaluator(standardTuning)
        )

        val emittedResults = tunerController.tuningResultStream().toList()

        assertEquals(listOf(null), emittedResults)
    }

    @Test
    fun emitsInTuneResultForAccuratelyDetectedString() = runTest {
        val eStringFrequencyHz = standardTuning.strings.first { it.name == "E" }.frequencyHz
        val audibleBuffer = SyntheticSignals.sine(eStringFrequencyHz, SAMPLE_RATE, SAMPLE_COUNT)
        val tunerController = TunerController(
            audioSource = FakePcmAudioSource(listOf(audibleBuffer)),
            silenceGate = SilenceGate(),
            pitchDetector = FakePitchDetector(listOf(eStringFrequencyHz)),
            pitchSmoother = PitchSmoother(),
            tuningEvaluator = TuningEvaluator(standardTuning)
        )

        val emittedResult = tunerController.tuningResultStream().toList().single()

        assertEquals(TuningStatus.IN_TUNE, emittedResult?.status)
        assertEquals("E", emittedResult?.closestString?.name)
    }

    @Test
    fun emitsFlatResultWhenDetectedFrequencyIsBelowTarget() = runTest {
        val aStringFrequencyHz = standardTuning.strings.first { it.name == "A" }.frequencyHz
        val flatFrequencyHz = aStringFrequencyHz - 2.0
        val audibleBuffer = SyntheticSignals.sine(aStringFrequencyHz, SAMPLE_RATE, SAMPLE_COUNT)
        val tunerController = TunerController(
            audioSource = FakePcmAudioSource(listOf(audibleBuffer)),
            silenceGate = SilenceGate(),
            pitchDetector = FakePitchDetector(listOf(flatFrequencyHz)),
            pitchSmoother = PitchSmoother(),
            tuningEvaluator = TuningEvaluator(standardTuning)
        )

        val emittedResult = tunerController.tuningResultStream().toList().single()

        assertEquals(TuningStatus.FLAT, emittedResult?.status)
    }

    @Test
    fun smoothsOutlierReadingAcrossConsecutiveBuffers() = runTest {
        val dStringFrequencyHz = standardTuning.strings.first { it.name == "D" }.frequencyHz
        val octaveOutlierFrequencyHz = dStringFrequencyHz * 2.0
        val audibleBuffer = SyntheticSignals.sine(dStringFrequencyHz, SAMPLE_RATE, SAMPLE_COUNT)
        val tunerController = TunerController(
            audioSource = FakePcmAudioSource(listOf(audibleBuffer, audibleBuffer, audibleBuffer)),
            silenceGate = SilenceGate(),
            pitchDetector = FakePitchDetector(
                listOf(dStringFrequencyHz, octaveOutlierFrequencyHz, dStringFrequencyHz)
            ),
            pitchSmoother = PitchSmoother(),
            tuningEvaluator = TuningEvaluator(standardTuning)
        )

        val emittedResults = tunerController.tuningResultStream().toList()

        assertEquals(TuningStatus.IN_TUNE, emittedResults[2]?.status)
        assertEquals("D", emittedResults[2]?.closestString?.name)
    }

    private companion object {
        const val SAMPLE_RATE = 44100
        const val SAMPLE_COUNT = 4096
    }
}