package com.example.wavefret.common.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/**
 * Hears what the phone is playing (for example a video inside WaveFret)
 * directly, as clean digital audio, instead of through the microphone.
 * Phone speakers can't reproduce the lowest bass notes, so this hears
 * bass lines far better. Needs Android 10, a [MediaProjection] the player
 * approved, and RECORD_AUDIO. Nothing is saved by this class.
 *
 * Thin wrapper over the Android audio framework, verified on-device.
 *
 * @property mediaProjection The approved capture session. Type: MediaProjection
 * @property sampleRateHz Sample rate audio is delivered at. Type: Int
 * @property samplesPerBufferCount Samples per delivered buffer. Type: Int
 */
@RequiresApi(Build.VERSION_CODES.Q)
class PlaybackCapturePcmSource(
    private val mediaProjection: MediaProjection,
    override val sampleRateHz: Int,
    private val samplesPerBufferCount: Int = DEFAULT_SAMPLES_PER_BUFFER_COUNT
) : PcmAudioSource {

    /**
     * @return A stream of captured audio buffers, mono, in -1..1. Type: Flow<FloatArray>
     */
    @SuppressLint("MissingPermission")
    override fun audioBufferStream(): Flow<FloatArray> = callbackFlow {
        val captureConfiguration = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        val minimumBufferSizeBytes = AudioRecord.getMinBufferSize(sampleRateHz, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val audioRecord = AudioRecord.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRateHz)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minimumBufferSizeBytes, samplesPerBufferCount * BYTES_PER_SAMPLE))
            .setAudioPlaybackCaptureConfig(captureConfiguration)
            .build()
        audioRecord.startRecording()
        val pcmReadBuffer = ShortArray(samplesPerBufferCount)
        try {
            while (isActive) {
                val samplesReadCount = audioRecord.read(pcmReadBuffer, 0, pcmReadBuffer.size)
                if (samplesReadCount > 0) {
                    trySend(FloatArray(samplesReadCount) { index -> pcmReadBuffer[index] / Short.MAX_VALUE.toFloat() })
                }
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val DEFAULT_SAMPLES_PER_BUFFER_COUNT = 2048
        const val BYTES_PER_SAMPLE = 2
    }
}
