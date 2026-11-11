package com.example.wavefret.common.audio

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavFileWriterTest {

    private fun written(samples: FloatArray, sampleRateHz: Int): ByteBuffer {
        val output = ByteArrayOutputStream()
        WavFileWriter().write(samples, sampleRateHz, output)
        return ByteBuffer.wrap(output.toByteArray()).order(ByteOrder.LITTLE_ENDIAN)
    }

    private fun ByteBuffer.ascii(offset: Int) = String(ByteArray(4) { get(offset + it) }, Charsets.US_ASCII)

    @Test
    fun headerDescribesSixteenBitMonoPcm() {
        val wav = written(FloatArray(10), 22_050)

        assertEquals("RIFF", wav.ascii(0))
        assertEquals("WAVE", wav.ascii(8))
        assertEquals("fmt ", wav.ascii(12))
        assertEquals(1, wav.getShort(20).toInt())        // PCM
        assertEquals(1, wav.getShort(22).toInt())        // mono
        assertEquals(22_050, wav.getInt(24))             // sample rate
        assertEquals(44_100, wav.getInt(28))             // byte rate
        assertEquals(16, wav.getShort(34).toInt())       // bits per sample
        assertEquals("data", wav.ascii(36))
        assertEquals(20, wav.getInt(40))                 // data bytes
        assertEquals(36 + 20, wav.getInt(4))             // RIFF chunk size
    }

    @Test
    fun samplesAreScaledAndClipped() {
        val wav = written(floatArrayOf(0f, 1f, -1f, 2f, 0.5f), 8_000)

        assertEquals(listOf(0, 32_767, -32_767, 32_767, 16_384), (0 until 5).map { wav.getShort(44 + 2 * it).toInt() })
    }
}
