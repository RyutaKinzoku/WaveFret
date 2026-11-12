package com.example.wavefret.tuner

import com.example.wavefret.common.audio.FakePcmAudioSource
import com.example.wavefret.common.audio.FakePitchDetector
import com.example.wavefret.common.audio.SilenceGate
import com.example.wavefret.common.audio.SyntheticSignals
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TunerControllerTest {

    private val audibleBuffer = SyntheticSignals.sine(55.0, SAMPLE_RATE, SAMPLE_COUNT)
    private val silentBuffer = SyntheticSignals.silence(SAMPLE_COUNT)

    private fun controllerFor(buffers: List<FloatArray>, scriptedFrequenciesHz: List<Double?>) = TunerController(
        audioSource = FakePcmAudioSource(buffers, SAMPLE_RATE),
        signalGate = SilenceGate(),
        pitchDetector = FakePitchDetector(scriptedFrequenciesHz)
    )

    @Test
    fun emitsOneReadingPerBuffer() = runTest {
        val readings = controllerFor(listOf(audibleBuffer, audibleBuffer, audibleBuffer), listOf(55.0, 55.1, 54.9))
            .pitchReadingStream().toList()

        assertEquals(listOf(55.0, 55.1, 54.9), readings)
    }

    @Test
    fun silenceIsReportedAsNoPitchWithoutRunningDetection() = runTest {
        val readings = controllerFor(listOf(silentBuffer, audibleBuffer), listOf(41.2)).pitchReadingStream().toList()

        assertEquals(listOf(null, 41.2), readings)
    }

    @Test
    fun undetectablePitchIsReportedAsNoPitch() = runTest {
        assertEquals(listOf<Double?>(null), controllerFor(listOf(audibleBuffer), listOf(null)).pitchReadingStream().toList())
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val SAMPLE_COUNT = 4096
    }
}
