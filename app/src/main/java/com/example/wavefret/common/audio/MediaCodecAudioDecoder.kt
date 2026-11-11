package com.example.wavefret.common.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.nio.ByteOrder

/**
 * Decoded audio, mono.
 *
 * @property samples Mono samples in -1..1. Type: FloatArray
 * @property sampleRateHz Their sample rate. Type: Int
 */
class DecodedAudio(val samples: FloatArray, val sampleRateHz: Int)

/**
 * Decodes any audio file Android can play (MP3, AAC/M4A, WAV, OGG, FLAC…)
 * into mono samples at a target rate, downmixing and resampling chunk by
 * chunk so long songs don't need a full-rate copy in memory. Stops after
 * [maximumMillis] to bound memory use.
 *
 * Thin wrapper over MediaExtractor/MediaCodec, verified on-device; the
 * downmix/resample maths is tested in [AudioResampler].
 *
 * @property context Context used to open content URIs. Type: Context
 * @property audioResampler Downmixes and resamples each chunk. Type: AudioResampler
 * @property targetSampleRateHz Rate of the returned samples. Type: Int
 * @property maximumMillis Longest stretch of audio decoded. Type: Long
 */
class MediaCodecAudioDecoder(
    private val context: Context,
    private val audioResampler: AudioResampler = AudioResampler(),
    private val targetSampleRateHz: Int = DEFAULT_TARGET_SAMPLE_RATE_HZ,
    private val maximumMillis: Long = DEFAULT_MAXIMUM_MILLIS
) {

    /**
     * @param audioUri Audio file to decode. Type: Uri
     * @return The decoded mono audio. Type: DecodedAudio
     * @throws IllegalArgumentException If the file has no decodable audio track.
     */
    fun decode(audioUri: Uri): DecodedAudio {
        val mediaExtractor = MediaExtractor()
        try {
            mediaExtractor.setDataSource(context, audioUri, null)
            val audioTrackIndex = (0 until mediaExtractor.trackCount).firstOrNull { trackIndex ->
                mediaExtractor.getTrackFormat(trackIndex).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IllegalArgumentException("This file has no audio")
            mediaExtractor.selectTrack(audioTrackIndex)
            val inputFormat = mediaExtractor.getTrackFormat(audioTrackIndex)
            val mimeType = requireNotNull(inputFormat.getString(MediaFormat.KEY_MIME))
            val mediaCodec = MediaCodec.createDecoderByType(mimeType)
            try {
                mediaCodec.configure(inputFormat, null, null, 0)
                mediaCodec.start()
                return DecodedAudio(drain(mediaExtractor, mediaCodec, inputFormat), targetSampleRateHz)
            } finally {
                mediaCodec.stop()
                mediaCodec.release()
            }
        } catch (unsupportedAudio: java.io.IOException) {
            throw IllegalArgumentException("This audio file can't be read", unsupportedAudio)
        } catch (codecFailure: IllegalStateException) {
            throw IllegalArgumentException("This audio format isn't supported on this phone", codecFailure)
        } finally {
            mediaExtractor.release()
        }
    }

    /**
     * Feeds the decoder and collects its output until the end of the file
     * or [maximumMillis].
     *
     * @param mediaExtractor Source of compressed audio. Type: MediaExtractor
     * @param mediaCodec Started decoder. Type: MediaCodec
     * @param inputFormat Track format, for the initial channel count and rate. Type: MediaFormat
     * @return Mono samples at the target rate. Type: FloatArray
     */
    private fun drain(mediaExtractor: MediaExtractor, mediaCodec: MediaCodec, inputFormat: MediaFormat): FloatArray {
        var channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var sourceRateHz = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var isFloatPcm = false
        val maximumSamples = (targetSampleRateHz * maximumMillis / MILLIS_PER_SECOND).toInt()
        val output = FloatArrayBuilder()
        val bufferInfo = MediaCodec.BufferInfo()
        var isInputDone = false
        while (output.size < maximumSamples) {
            if (!isInputDone) {
                val inputIndex = mediaCodec.dequeueInputBuffer(CODEC_TIMEOUT_MICROS)
                if (inputIndex >= 0) {
                    val inputBuffer = requireNotNull(mediaCodec.getInputBuffer(inputIndex))
                    val readSize = mediaExtractor.readSampleData(inputBuffer, 0)
                    if (readSize < 0) {
                        mediaCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isInputDone = true
                    } else {
                        mediaCodec.queueInputBuffer(inputIndex, 0, readSize, mediaExtractor.sampleTime, 0)
                        mediaExtractor.advance()
                    }
                }
            }
            val outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_MICROS)
            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val outputFormat = mediaCodec.outputFormat
                    channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    sourceRateHz = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    isFloatPcm = outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                        outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                }
                outputIndex >= 0 -> {
                    val outputBuffer = requireNotNull(mediaCodec.getOutputBuffer(outputIndex)).order(ByteOrder.nativeOrder())
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    val interleaved = if (isFloatPcm) {
                        FloatArray(bufferInfo.size / FLOAT_BYTES).also { outputBuffer.asFloatBuffer().get(it) }
                    } else {
                        val pcm = ShortArray(bufferInfo.size / SHORT_BYTES).also { outputBuffer.asShortBuffer().get(it) }
                        FloatArray(pcm.size) { pcm[it] / Short.MAX_VALUE.toFloat() }
                    }
                    mediaCodec.releaseOutputBuffer(outputIndex, false)
                    val mono = audioResampler.toMono(interleaved, channelCount)
                    output.append(audioResampler.resample(mono, sourceRateHz, targetSampleRateHz))
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
        }
        return output.toFloatArray(maximumSamples)
    }

    /** Growable float buffer, to avoid boxing millions of samples. */
    private class FloatArrayBuilder {
        private var buffer = FloatArray(INITIAL_CAPACITY)
        var size = 0
            private set

        fun append(samples: FloatArray) {
            if (size + samples.size > buffer.size) buffer = buffer.copyOf(maxOf(buffer.size * 2, size + samples.size))
            samples.copyInto(buffer, size)
            size += samples.size
        }

        fun toFloatArray(maximumSize: Int): FloatArray = buffer.copyOf(minOf(size, maximumSize))

        private companion object {
            const val INITIAL_CAPACITY = 1 shl 16
        }
    }

    private companion object {
        const val DEFAULT_TARGET_SAMPLE_RATE_HZ = 22_050
        const val DEFAULT_MAXIMUM_MILLIS = 6 * 60 * 1_000L
        const val MILLIS_PER_SECOND = 1_000L
        const val CODEC_TIMEOUT_MICROS = 10_000L
        const val FLOAT_BYTES = 4
        const val SHORT_BYTES = 2
    }
}
