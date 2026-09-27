package com.example.wavefret.tuner

import com.example.wavefret.common.audio.PcmAudioSource
import com.example.wavefret.common.audio.PitchDetector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Orchestrates the live tuning pipeline: gates out silence, detects the
 * dominant pitch in each audio buffer, smooths it against jitter and
 * octave-jump outliers, then evaluates it against a target tuning.
 *
 * @param audioSource Where live audio buffers come from. Type: PcmAudioSource
 * @param silenceGate Filters out buffers with no meaningful signal. Type: SilenceGate
 * @param pitchDetector Estimates the fundamental frequency of a buffer. Type: PitchDetector
 * @param pitchSmoother Smooths successive frequency readings over time. Type: PitchSmoother
 * @param tuningEvaluator Compares a frequency against the target tuning. Type: TuningEvaluator
 */
class TunerController(
    private val audioSource: PcmAudioSource,
    private val silenceGate: SilenceGate,
    private val pitchDetector: PitchDetector,
    private val pitchSmoother: PitchSmoother,
    private val tuningEvaluator: TuningEvaluator
) {

    /**
     * Converts the raw audio buffer stream into a stream of tuning
     * results, one per buffer, with null standing in for "nothing
     * confident to report" (silence, or no clear pitch).
     *
     * @return A stream of tuning results, or null per buffer with no usable pitch. Type: Flow<TuningResult?>
     */
    fun tuningResultStream(): Flow<TuningResult?> {
        return audioSource.audioBufferStream().map { audioBuffer -> evaluateBuffer(audioBuffer) }
    }

    /**
     * Runs a single audio buffer through the full pipeline.
     *
     * @param audioBuffer One buffer of normalized audio samples. Type: FloatArray
     * @return The tuning result for this buffer, or null if nothing usable was found. Type: TuningResult?
     */
    private fun evaluateBuffer(audioBuffer: FloatArray): TuningResult? {
        if (!silenceGate.hasSignal(audioBuffer)) return null
        val detectedFrequencyHz = pitchDetector.detectPitch(audioBuffer, audioSource.sampleRateHz) ?: return null
        val smoothedFrequencyHz = pitchSmoother.addReading(detectedFrequencyHz)
        return tuningEvaluator.evaluate(smoothedFrequencyHz)
    }
}