package com.example.wavefret.practice

import com.example.wavefret.common.audio.AttackDetector
import com.example.wavefret.common.audio.PcmAudioSource
import com.example.wavefret.common.audio.PitchDetector
import com.example.wavefret.common.audio.SignalGate
import com.example.wavefret.common.audio.TargetedPitchDetector
import com.example.wavefret.common.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Orchestrates the live practice pipeline: gates out silence, listens for
 * the note the session expects (which hears soft playing), otherwise
 * detects whatever pitch is there (for "wrong note" feedback), timestamps
 * the reading and feeds it to the session, which decides whether the
 * expected note was played.
 *
 * @param audioSource Where live audio buffers come from. Type: PcmAudioSource
 * @param signalGate Filters out buffers with no meaningful signal. Type: SignalGate
 * @param pitchDetector Estimates the fundamental frequency of a buffer. Type: PitchDetector
 * @param clock Monotonic time source used to timestamp each buffer. Type: Clock
 * @param targetedPitchDetector Listens specifically for the expected note, or null to skip. Type: TargetedPitchDetector?
 * @param attackDetector Tells the session when a new pluck starts. Type: AttackDetector
 */
class PracticeController(
    private val audioSource: PcmAudioSource,
    private val signalGate: SignalGate,
    private val pitchDetector: PitchDetector,
    private val clock: Clock,
    private val targetedPitchDetector: TargetedPitchDetector? = null,
    private val attackDetector: AttackDetector = AttackDetector()
) {

    /**
     * @param practiceSession Session that judges each reading. Type: PracticeSession
     * @return A stream with the session's progress after every buffer. Type: Flow<PracticeProgress>
     */
    fun progressStream(practiceSession: PracticeSession): Flow<PracticeProgress> {
        return audioSource.audioBufferStream().map { audioBuffer ->
            if (attackDetector.onLevel(rootMeanSquare(audioBuffer))) practiceSession.onAttack()
            practiceSession.onPitchDetected(detectPitchOrNull(audioBuffer, practiceSession), clock.currentTimeMillis())
        }
    }

    /**
     * @param audioBuffer Audio window. Type: FloatArray
     * @return Its root-mean-square level. Type: Double
     */
    private fun rootMeanSquare(audioBuffer: FloatArray): Double {
        if (audioBuffer.isEmpty()) return 0.0
        var sumOfSquares = 0.0
        for (sample in audioBuffer) sumOfSquares += sample * sample.toDouble()
        return kotlin.math.sqrt(sumOfSquares / audioBuffer.size)
    }

    /**
     * @param audioBuffer One buffer of normalized audio samples. Type: FloatArray
     * @param practiceSession Session whose expected note to listen for. Type: PracticeSession
     * @return The detected pitch in Hz, or null for silence or no clear pitch. Type: Double?
     */
    private fun detectPitchOrNull(audioBuffer: FloatArray, practiceSession: PracticeSession): Double? {
        if (!signalGate.hasSignal(audioBuffer)) return null
        val expectedFrequencyHz = practiceSession.expectedFrequencyHz()
        if (targetedPitchDetector != null && expectedFrequencyHz != null) {
            targetedPitchDetector.detectNear(audioBuffer, audioSource.sampleRateHz, expectedFrequencyHz)?.let { return it }
        }
        return pitchDetector.detectPitch(audioBuffer, audioSource.sampleRateHz)
    }
}
