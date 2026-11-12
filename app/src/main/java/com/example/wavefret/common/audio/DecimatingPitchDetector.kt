package com.example.wavefret.common.audio

/**
 * Speeds up pitch detection on bass: everything above a few hundred hertz is
 * filtered away and only every Nth sample is kept, so the detector does a
 * fraction of the work per reading and readings can come many times a second.
 *
 * @property innerPitchDetector Detector run on the reduced audio. Type: PitchDetector
 * @property decimationFactor Keep one sample in this many. Type: Int
 */
class DecimatingPitchDetector(
    private val innerPitchDetector: PitchDetector,
    private val decimationFactor: Int = DEFAULT_DECIMATION_FACTOR
) : PitchDetector {

    init {
        require(decimationFactor >= 1) { "Decimation factor must be at least 1" }
    }

    /**
     * @param samples Audio buffer in -1..1. Type: FloatArray
     * @param sampleRate Its sample rate. Type: Int
     * @return Detected pitch in Hz, or null. Type: Double?
     */
    override fun detectPitch(samples: FloatArray, sampleRate: Int): Double? {
        val reducedRateHz = sampleRate / decimationFactor
        val antiAliased = LowPassFilter(cutoffHz = reducedRateHz * ANTI_ALIAS_FRACTION_OF_REDUCED_RATE).filter(samples, sampleRate)
        val reduced = FloatArray(antiAliased.size / decimationFactor) { index -> antiAliased[index * decimationFactor] }
        return innerPitchDetector.detectPitch(reduced, reducedRateHz)
    }

    private companion object {
        const val DEFAULT_DECIMATION_FACTOR = 4
        /** Below the reduced Nyquist frequency (0.5) with room for the filter's slope. */
        const val ANTI_ALIAS_FRACTION_OF_REDUCED_RATE = 0.2
    }
}
