package com.example.wavefret.tuner

import com.example.wavefret.common.audio.PcmAudioSource
import com.example.wavefret.common.audio.PitchDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

/**
 * Deterministic stand-in for a real audio source, replaying a fixed
 * list of buffers instead of capturing from a microphone.
 *
 * @param audioBuffers The buffers to emit, in order. Type: List<FloatArray>
 * @param sampleRateHz The sample rate to report. Type: Int
 */
class FakePcmAudioSource(
    private val audioBuffers: List<FloatArray>,
    override val sampleRateHz: Int = 44100
) : PcmAudioSource {

    /**
     * Replays the configured buffers as a finite flow.
     *
     * @return The configured buffers, in order. Type: Flow<FloatArray>
     */
    override fun audioBufferStream(): Flow<FloatArray> = audioBuffers.asFlow()
}

/**
 * Deterministic stand-in for a real pitch detector, returning a
 * pre-scripted frequency (or null) for each successive call instead of
 * analyzing the samples it's given.
 *
 * @param scriptedFrequenciesHz One result per expected call, in order. Type: List<Double?>
 */
class FakePitchDetector(
    private val scriptedFrequenciesHz: List<Double?>
) : PitchDetector {

    private var callCount = 0

    /**
     * Returns the next scripted frequency, ignoring the actual samples.
     *
     * @param samples Unused. Type: FloatArray
     * @param sampleRate Unused. Type: Int
     * @return The next scripted frequency, or null past the end of the script. Type: Double?
     */
    override fun detectPitch(samples: FloatArray, sampleRate: Int): Double? {
        val scriptedFrequencyHz = scriptedFrequenciesHz.getOrNull(callCount)
        callCount++
        return scriptedFrequencyHz
    }
}