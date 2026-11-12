package com.example.wavefret.tuner

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Plays a short, soft two-note chime (a rising fifth) when a string is
 * confirmed in tune. The sound is synthesised once and replayed.
 *
 * Thin wrapper over AudioTrack, verified on-device.
 */
class TuneChimePlayer {

    private val chimeSamples = synthesizeChime()
    private val audioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE_HZ)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setTransferMode(AudioTrack.MODE_STATIC)
        .setBufferSizeInBytes(chimeSamples.size * BYTES_PER_SAMPLE)
        .build()
        .also { it.write(chimeSamples, 0, chimeSamples.size) }

    /**
     * @return Unit
     */
    fun play() {
        audioTrack.stop()
        audioTrack.reloadStaticData()
        audioTrack.play()
    }

    /**
     * @return Unit
     */
    fun release() = audioTrack.release()

    private companion object {
        const val SAMPLE_RATE_HZ = 44_100
        const val BYTES_PER_SAMPLE = 2
        const val NOTE_SECONDS = 0.18
        const val DECAY_PER_SECOND = 9.0
        const val PEAK_LEVEL = 0.35
        val CHIME_FREQUENCIES_HZ = listOf(880.0, 1_318.51)

        /** @return Both notes back to back, each a decaying sine, as 16-bit PCM. Type: ShortArray */
        fun synthesizeChime(): ShortArray {
            val samplesPerNote = (SAMPLE_RATE_HZ * NOTE_SECONDS).toInt()
            return ShortArray(samplesPerNote * CHIME_FREQUENCIES_HZ.size) { sampleIndex ->
                val frequencyHz = CHIME_FREQUENCIES_HZ[sampleIndex / samplesPerNote]
                val seconds = (sampleIndex % samplesPerNote).toDouble() / SAMPLE_RATE_HZ
                val level = PEAK_LEVEL * exp(-DECAY_PER_SECOND * seconds) * sin(2 * PI * frequencyHz * seconds)
                (level * Short.MAX_VALUE).toInt().toShort()
            }
        }
    }
}
