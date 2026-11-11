package com.example.wavefret.practice

import com.example.wavefret.common.audio.PcmAudioSource
import com.example.wavefret.common.audio.PitchDetector
import com.example.wavefret.common.audio.SilenceGate
import com.example.wavefret.common.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Orchestrates the live practice pipeline: gates out silence, detects the
 * pitch of each audio buffer, timestamps it, and feeds it to a practice
 * session, which decides whether the expected note was played.
 *
 * @param audioSource Where live audio buffers come from. Type: PcmAudioSource
 * @param silenceGate Filters out buffers with no meaningful signal. Type: SilenceGate
 * @param pitchDetector Estimates the fundamental frequency of a buffer. Type: PitchDetector
 * @param clock Monotonic time source used to timestamp each buffer. Type: Clock
 */
class PracticeController(
    private val audioSource: PcmAudioSource,
    private val silenceGate: SilenceGate,
    private val pitchDetector: PitchDetector,
    private val clock: Clock
) {

    /**
     * @param practiceSession Session that judges each reading. Type: PracticeSession
     * @return A stream with the session's progress after every buffer. Type: Flow<PracticeProgress>
     */
    fun progressStream(practiceSession: PracticeSession): Flow<PracticeProgress> {
        return audioSource.audioBufferStream().map { audioBuffer ->
            practiceSession.onPitchDetected(detectPitchOrNull(audioBuffer), clock.currentTimeMillis())
        }
    }

    /**
     * @param audioBuffer One buffer of normalized audio samples. Type: FloatArray
     * @return The detected pitch in Hz, or null for silence or no clear pitch. Type: Double?
     */
    private fun detectPitchOrNull(audioBuffer: FloatArray): Double? {
        if (!silenceGate.hasSignal(audioBuffer)) return null
        return pitchDetector.detectPitch(audioBuffer, audioSource.sampleRateHz)
    }
}
