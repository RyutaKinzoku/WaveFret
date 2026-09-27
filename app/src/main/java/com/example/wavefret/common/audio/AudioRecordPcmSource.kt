package com.example.wavefret.common.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/**
 * Real [PcmAudioSource] backed by [AudioRecord]. Prefers
 * [MediaRecorder.AudioSource.UNPROCESSED] over the default microphone
 * source because Android's standard mic path applies speech-tuned
 * noise suppression that attenuates a bass guitar's low fundamental;
 * falls back to [MediaRecorder.AudioSource.VOICE_RECOGNITION] on
 * devices that don't support UNPROCESSED.
 *
 * This class is a thin wrapper over the Android audio framework with
 * no meaningful decision logic of its own, so per this project's
 * convention it's verified manually on-device rather than in JUnit.
 *
 * @param sampleRateHz The capture sample rate. Type: Int
 * @param samplesPerBufferCount How many samples to emit per buffer. Type: Int
 */
class AudioRecordPcmSource(
    override val sampleRateHz: Int = DEFAULT_SAMPLE_RATE_HZ,
    private val samplesPerBufferCount: Int = DEFAULT_SAMPLES_PER_BUFFER_COUNT
) : PcmAudioSource {

    /**
     * Opens the microphone and emits normalized audio buffers until the
     * collecting coroutine is canceled.
     *
     * @return A cold stream of normalized (-1.0..1.0) audio sample buffers. Type: Flow<FloatArray>
     */
    override fun audioBufferStream(): Flow<FloatArray> = callbackFlow {
        val audioRecord = createAudioRecord()
        audioRecord.startRecording()
        val pcmReadBuffer = ShortArray(samplesPerBufferCount)
        try {
            while (isActive) {
                val samplesReadCount = audioRecord.read(pcmReadBuffer, 0, pcmReadBuffer.size)
                if (samplesReadCount > 0) {
                    trySend(normalizeToFloatSamples(pcmReadBuffer, samplesReadCount))
                }
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    /**
     * Builds an [AudioRecord] on [MediaRecorder.AudioSource.UNPROCESSED]
     * when the device supports it, otherwise falls back to
     * [MediaRecorder.AudioSource.VOICE_RECOGNITION].
     *
     * @return A configured [AudioRecord], ready to start. Type: AudioRecord
     */
    private fun createAudioRecord(): AudioRecord {
        val minimumBufferSizeBytes = AudioRecord.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSizeBytes = maxOf(minimumBufferSizeBytes, samplesPerBufferCount * BYTES_PER_SAMPLE)

        val preferredAudioRecord = buildAudioRecord(MediaRecorder.AudioSource.UNPROCESSED, bufferSizeBytes)
        if (preferredAudioRecord.state == AudioRecord.STATE_INITIALIZED) return preferredAudioRecord
        preferredAudioRecord.release()

        return buildAudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, bufferSizeBytes)
    }

    /**
     * Constructs an [AudioRecord] for the given audio source.
     *
     * Callers of [audioBufferStream] are required to have already
     * secured RECORD_AUDIO before starting the pipeline (see
     * `MicrophonePermissionManager`). A [SecurityException] here means
     * that precondition was violated, not a normal, recoverable outcome.
     * It's caught only so the failure surfaces as an explicit,
     * traceable error instead of an unguarded crash.
     *
     * @param audioSourceConstant One of [MediaRecorder.AudioSource]'s constants. Type: Int
     * @param bufferSizeBytes The internal buffer size to allocate. Type: Int
     * @return The constructed AudioRecord. Type: AudioRecord
     */
    private fun buildAudioRecord(audioSourceConstant: Int, bufferSizeBytes: Int): AudioRecord {
        return try {
            AudioRecord(
                audioSourceConstant,
                sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeBytes
            )
        } catch (missingRecordAudioPermission: SecurityException) {
            throw IllegalStateException(
                "AudioRecordPcmSource requires RECORD_AUDIO to already be granted",
                missingRecordAudioPermission
            )
        }
    }

    /**
     * Converts signed 16-bit PCM samples to the -1.0..1.0 float range
     * the rest of the pipeline expects.
     *
     * @param pcmReadBuffer The raw buffer read from the microphone. Type: ShortArray
     * @param samplesReadCount How many samples in [pcmReadBuffer] are valid. Type: Int
     * @return The normalized samples. Type: FloatArray
     */
    private fun normalizeToFloatSamples(pcmReadBuffer: ShortArray, samplesReadCount: Int): FloatArray {
        return FloatArray(samplesReadCount) { index -> pcmReadBuffer[index] / Short.MAX_VALUE.toFloat() }
    }

    private companion object {
        const val DEFAULT_SAMPLE_RATE_HZ = 44100
        const val DEFAULT_SAMPLES_PER_BUFFER_COUNT = 4096
        const val BYTES_PER_SAMPLE = 2
    }
}