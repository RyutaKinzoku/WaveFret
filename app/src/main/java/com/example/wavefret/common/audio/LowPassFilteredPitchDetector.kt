package com.example.wavefret.common.audio

/**
 * Decorator that low-pass filters every buffer before handing it to
 * another [PitchDetector]. Filtering and period-finding stay separate,
 * separately tested responsibilities, and any detector gains noise
 * robustness without its own code changing.
 *
 * @property lowPassFilter Filter applied to each buffer first. Type: LowPassFilter
 * @property innerPitchDetector Detector that analyzes the filtered buffer. Type: PitchDetector
 */
class LowPassFilteredPitchDetector(
    private val lowPassFilter: LowPassFilter,
    private val innerPitchDetector: PitchDetector
) : PitchDetector {

    /**
     * @param samples Audio buffer to analyze, as PCM float samples in [-1, 1]. Type: FloatArray
     * @param sampleRate Sample rate the buffer was captured at, in Hz. Type: Int
     * @return Whatever the inner detector finds in the filtered buffer. Type: Double?
     */
    override fun detectPitch(samples: FloatArray, sampleRate: Int): Double? {
        val filteredSamples = lowPassFilter.filter(samples, sampleRate)
        return innerPitchDetector.detectPitch(filteredSamples, sampleRate)
    }
}