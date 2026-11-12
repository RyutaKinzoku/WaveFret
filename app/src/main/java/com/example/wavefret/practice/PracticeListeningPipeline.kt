package com.example.wavefret.practice

import com.example.wavefret.common.audio.AdaptiveSilenceGate
import com.example.wavefret.common.audio.AudioRecordPcmSource
import com.example.wavefret.common.audio.DecimatingPitchDetector
import com.example.wavefret.common.audio.LowPassFilter
import com.example.wavefret.common.audio.LowPassFilteredPitchDetector
import com.example.wavefret.common.audio.SlidingWindowAudioSource
import com.example.wavefret.common.audio.TargetedPitchDetector
import com.example.wavefret.common.audio.YinPitchDetector
import com.example.wavefret.common.time.Clock

/**
 * Builds the microphone pipeline every practice mode listens through,
 * tuned for relaxed, soft playing: a reading every 46 ms over a 93 ms
 * window (long enough for low E), a noise gate that adapts to the room,
 * and a detector that listens specifically for the expected note.
 *
 * Composition glue around tested parts; verified on-device.
 */
object PracticeListeningPipeline {

    private const val CAPTURE_SAMPLES = 1024
    private const val WINDOW_SAMPLES = 4096
    private const val HOP_SAMPLES = 2048
    private const val PITCH_ANALYSIS_CUTOFF_HZ = 1000.0

    /**
     * @param clock Monotonic clock used to timestamp readings. Type: Clock
     * @return A controller listening to the microphone. Type: PracticeController
     */
    fun createController(clock: Clock): PracticeController = PracticeController(
        audioSource = SlidingWindowAudioSource(AudioRecordPcmSource(samplesPerBufferCount = CAPTURE_SAMPLES), WINDOW_SAMPLES, HOP_SAMPLES),
        signalGate = AdaptiveSilenceGate(),
        pitchDetector = LowPassFilteredPitchDetector(
            lowPassFilter = LowPassFilter(cutoffHz = PITCH_ANALYSIS_CUTOFF_HZ),
            innerPitchDetector = DecimatingPitchDetector(YinPitchDetector())
        ),
        clock = clock,
        targetedPitchDetector = TargetedPitchDetector()
    )
}
