package com.example.wavefret.common.audio

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/** Writes mono float samples as a standard 16-bit PCM WAV file. */
class WavFileWriter {

    /**
     * @param samples Mono samples in -1..1; values outside are clipped. Type: FloatArray
     * @param sampleRateHz Their sample rate. Type: Int
     * @param output Where the file is written; not closed. Type: OutputStream
     * @return Unit
     */
    fun write(samples: FloatArray, sampleRateHz: Int, output: OutputStream) {
        val dataBytes = samples.size * BYTES_PER_SAMPLE
        val header = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII)); putInt(RIFF_HEADER_REMAINDER_BYTES + dataBytes)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII)); putInt(FORMAT_CHUNK_BYTES)
            putShort(PCM_FORMAT); putShort(MONO_CHANNELS)
            putInt(sampleRateHz); putInt(sampleRateHz * BYTES_PER_SAMPLE)
            putShort(BYTES_PER_SAMPLE.toShort()); putShort(BITS_PER_SAMPLE)
            put("data".toByteArray(Charsets.US_ASCII)); putInt(dataBytes)
        }
        output.write(header.array())
        val pcm = ByteBuffer.allocate(dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { sample -> pcm.putShort((sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort()) }
        output.write(pcm.array())
    }

    private companion object {
        const val HEADER_BYTES = 44
        const val RIFF_HEADER_REMAINDER_BYTES = 36
        const val FORMAT_CHUNK_BYTES = 16
        const val PCM_FORMAT: Short = 1
        const val MONO_CHANNELS: Short = 1
        const val BYTES_PER_SAMPLE = 2
        const val BITS_PER_SAMPLE: Short = 16
    }
}
