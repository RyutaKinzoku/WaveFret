package com.example.wavefret.practice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.wavefret.common.audio.PluckedStringSynthesizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Plays a lesson's notes on a synthesized bass, so the player can hear
 * how the lesson should sound and check that its notes are right.
 *
 * Android framework glue (AudioTrack) around the tested
 * [LessonPlaybackSchedule] and [PluckedStringSynthesizer]; verified on-device.
 *
 * @property lessonPlaybackSchedule Decides when each note sounds. Type: LessonPlaybackSchedule
 * @property synthesizer Makes each note's sound. Type: PluckedStringSynthesizer
 */
class LessonSynthPlayer(
    private val lessonPlaybackSchedule: LessonPlaybackSchedule = LessonPlaybackSchedule(),
    private val synthesizer: PluckedStringSynthesizer = PluckedStringSynthesizer()
) {

    /**
     * Plays until the lesson ends or the calling coroutine is cancelled,
     * which silences it at once.
     *
     * @param lesson Lesson to play. Type: Exercise
     * @param fromNoteIndex First note to play. Type: Int
     * @param onNote Called on the main thread with each note's index as it starts. Type: (Int) -> Unit
     * @return Unit
     */
    suspend fun play(lesson: Exercise, fromNoteIndex: Int, onNote: (Int) -> Unit) = withContext(Dispatchers.Default) {
        val minimumBufferBytes = AudioTrack.getMinBufferSize(SAMPLE_RATE_HZ, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE_HZ)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .build()
            )
            .setBufferSizeInBytes(minimumBufferBytes * BUFFER_SIZE_MULTIPLE)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        try {
            audioTrack.play()
            for (scheduledNote in lessonPlaybackSchedule.schedule(lesson, fromNoteIndex)) {
                val samples = synthesizer.pluck(
                    scheduledNote.frequencyHz,
                    (scheduledNote.durationMillis * SAMPLE_RATE_HZ / MILLIS_PER_SECOND).toInt(),
                    SAMPLE_RATE_HZ
                )
                withContext(Dispatchers.Main) { onNote(scheduledNote.noteIndex) }
                writeInChunks(audioTrack, samples)
            }
            delay(minimumBufferBytes * BUFFER_SIZE_MULTIPLE / BYTES_PER_SAMPLE * MILLIS_PER_SECOND / SAMPLE_RATE_HZ)
        } finally {
            audioTrack.pause()
            audioTrack.flush()
            audioTrack.release()
        }
    }

    /**
     * Writes a note a little at a time, so stopping takes effect quickly.
     *
     * @param audioTrack Track being played. Type: AudioTrack
     * @param samples The note. Type: FloatArray
     * @return Unit
     */
    private suspend fun writeInChunks(audioTrack: AudioTrack, samples: FloatArray) {
        var written = 0
        while (written < samples.size) {
            coroutineContext.ensureActive()
            val chunkSize = minOf(CHUNK_SAMPLES, samples.size - written)
            val writtenNow = audioTrack.write(samples, written, chunkSize, AudioTrack.WRITE_BLOCKING)
            if (writtenNow <= 0) return
            written += writtenNow
        }
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 44_100
        const val MILLIS_PER_SECOND = 1_000L
        const val BUFFER_SIZE_MULTIPLE = 2
        const val BYTES_PER_SAMPLE = 4
        const val CHUNK_SAMPLES = 2_205
    }
}
