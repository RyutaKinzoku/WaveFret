package com.example.wavefret.tuner

import com.example.wavefret.common.audio.PcmAudioSource
import com.example.wavefret.common.audio.PitchDetector
import com.example.wavefret.common.audio.SignalGate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The tuner's audio pipeline: gates out silence and detects the pitch of
 * each analysis window. What the readings mean (which string, how far off,
 * when it is tuned) is decided by [GuidedTuner].
 *
 * @param audioSource Where live audio windows come from. Type: PcmAudioSource
 * @param signalGate Filters out windows with no meaningful signal. Type: SignalGate
 * @param pitchDetector Estimates the pitch of a window. Type: PitchDetector
 */
class TunerController(
    private val audioSource: PcmAudioSource,
    private val signalGate: SignalGate,
    private val pitchDetector: PitchDetector
) {

    /**
     * @return One reading per audio window: the pitch in Hz, or null when nothing clear is heard. Type: Flow<Double?>
     */
    fun pitchReadingStream(): Flow<Double?> = audioSource.audioBufferStream().map { audioWindow ->
        if (signalGate.hasSignal(audioWindow)) pitchDetector.detectPitch(audioWindow, audioSource.sampleRateHz) else null
    }
}
