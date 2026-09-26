package com.example.wavefret.common.audio

/**
 * Abstraction over fundamental-frequency detection, so anything that
 * needs a pitch (the tuner, later the practice engine) can depend on this
 * contract rather than a specific algorithm — and so a deterministic fake
 * can stand in for real pitch detection in tests that aren't about the
 * algorithm itself.
 */
interface PitchDetector {
    /**
     * @param samples Audio buffer to analyze, as PCM float samples in [-1, 1]. Type: FloatArray
     * @param sampleRate Sample rate the buffer was captured at, in Hz. Type: Int
     * @return Detected fundamental frequency in Hz, or null if no periodic pitch was found. Type: Double?
     */
    fun detectPitch(samples: FloatArray, sampleRate: Int): Double?
}