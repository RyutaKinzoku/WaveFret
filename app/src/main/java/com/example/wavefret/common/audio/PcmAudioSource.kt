package com.example.wavefret.common.audio

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over a live source of raw PCM audio samples, decoupling
 * pitch-detection logic from the concrete way audio is captured
 * (microphone hardware, a fake for tests, etc).
 */
interface PcmAudioSource {

    /**
     * The sample rate the emitted buffers are recorded at.
     *
     * Type: Int
     */
    val sampleRateHz: Int

    /**
     * Starts capturing audio and exposes each captured buffer as it
     * becomes available.
     *
     * @return A cold stream of normalized (-1.0..1.0) audio sample buffers. Type: Flow<FloatArray>
     */
    fun audioBufferStream(): Flow<FloatArray>
}