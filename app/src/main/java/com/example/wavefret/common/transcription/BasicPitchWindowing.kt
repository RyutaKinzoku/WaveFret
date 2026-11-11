package com.example.wavefret.common.transcription

import kotlin.math.floor

/**
 * Cuts audio into the overlapping two-second windows Spotify's basic-pitch
 * model takes, and stitches the model's per-window frame maps back into one
 * map aligned with the original audio, following basic-pitch's own
 * inference code (half an overlap of silence in front, half the overlapping
 * frames dropped on each side of every window).
 */
class BasicPitchWindowing {

    /**
     * @param audio Mono samples at [SAMPLE_RATE_HZ]. Type: FloatArray
     * @return Model inputs, each exactly [WINDOW_SAMPLES] long. Type: List<FloatArray>
     */
    fun windows(audio: FloatArray): List<FloatArray> {
        val paddedLength = OVERLAP_SAMPLES / 2 + audio.size
        val windows = mutableListOf<FloatArray>()
        var windowStart = 0
        while (windowStart < paddedLength) {
            val window = FloatArray(WINDOW_SAMPLES)
            for (windowIndex in 0 until WINDOW_SAMPLES) {
                val audioIndex = windowStart + windowIndex - OVERLAP_SAMPLES / 2
                if (audioIndex in audio.indices) window[windowIndex] = audio[audioIndex]
            }
            windows += window
            windowStart += HOP_SAMPLES
        }
        return windows
    }

    /**
     * @param windowOutputs One frames-by-bins map per window, in order. Type: List<Array<FloatArray>>
     * @param audioSamples Length of the original audio. Type: Int
     * @return One continuous frames-by-bins map for the whole audio. Type: Array<FloatArray>
     */
    fun stitch(windowOutputs: List<Array<FloatArray>>, audioSamples: Int): Array<FloatArray> {
        val trimFrames = OVERLAP_FRAMES / 2
        val keptFrames = windowOutputs.flatMap { windowOutput ->
            windowOutput.copyOfRange(trimFrames, windowOutput.size - trimFrames).asList()
        }
        return keptFrames.take(frameCountFor(audioSamples)).toTypedArray()
    }

    /**
     * @param audioSamples Length of the audio. Type: Int
     * @return How many model frames cover it. Type: Int
     */
    fun frameCountFor(audioSamples: Int): Int = floor(audioSamples.toDouble() / HOP_LENGTH_SAMPLES).toInt()

    companion object {
        /** Sample rate the model expects. Type: Int */
        const val SAMPLE_RATE_HZ = 22_050

        /** Samples between model frames. Type: Int */
        const val HOP_LENGTH_SAMPLES = 256

        /** Model input length: two seconds minus one hop. Type: Int */
        const val WINDOW_SAMPLES = 2 * SAMPLE_RATE_HZ - HOP_LENGTH_SAMPLES

        /** Frames the model outputs per window. Type: Int */
        const val FRAMES_PER_WINDOW = 172

        /** Frames shared by consecutive windows. Type: Int */
        const val OVERLAP_FRAMES = 30

        /** Samples shared by consecutive windows. Type: Int */
        const val OVERLAP_SAMPLES = OVERLAP_FRAMES * HOP_LENGTH_SAMPLES

        /** Distance between window starts. Type: Int */
        const val HOP_SAMPLES = WINDOW_SAMPLES - OVERLAP_SAMPLES

        /** Model frames per second of audio. Type: Double */
        const val FRAMES_PER_SECOND = SAMPLE_RATE_HZ.toDouble() / HOP_LENGTH_SAMPLES
    }
}
