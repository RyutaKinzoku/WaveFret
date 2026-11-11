package com.example.wavefret.common.transcription

import com.example.wavefret.common.audio.PitchDetector
import com.example.wavefret.common.music.NoteEvent
import com.example.wavefret.common.music.centsBetween
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Transcribes audio with one note at a time, such as a bass played on its
 * own, using the app's YIN pitch detector frame by frame. Frame pitches are
 * median-smoothed, then runs of the same note become notes; silence or a
 * pitch change ends a note. Accurate on clean bass, unsuitable for full mixes.
 *
 * @property pitchDetector Finds the pitch of each analysis window. Type: PitchDetector
 * @property silenceThreshold RMS of a frame's centre below which it counts as silent. Type: Double
 * @property minimumNoteMillis Shorter runs are discarded as noise. Type: Long
 */
class MonophonicTranscriber(
    private val pitchDetector: PitchDetector,
    private val silenceThreshold: Double = DEFAULT_SILENCE_THRESHOLD,
    private val minimumNoteMillis: Long = DEFAULT_MINIMUM_NOTE_MILLIS
) : NoteTranscriber {

    /**
     * @param samples Mono audio samples in -1..1. Type: FloatArray
     * @param sampleRateHz Their sample rate. Type: Int
     * @return Detected notes in time order. Type: List<NoteEvent>
     */
    override fun transcribe(samples: FloatArray, sampleRateHz: Int): List<NoteEvent> {
        val windowSamples = sampleRateHz * WINDOW_MILLIS / MILLIS_PER_SECOND
        val hopSamples = sampleRateHz * HOP_MILLIS / MILLIS_PER_SECOND
        if (samples.size < windowSamples) return emptyList()
        val frameCount = (samples.size - windowSamples) / hopSamples + 1
        val rawFrameNotes = List(frameCount) { frame ->
            val windowStart = frame * hopSamples
            val centreStart = windowStart + windowSamples / 2 - hopSamples
            if (rootMeanSquare(samples, centreStart, 2 * hopSamples) < silenceThreshold) {
                null
            } else {
                pitchDetector.detectPitch(samples.copyOfRange(windowStart, windowStart + windowSamples), sampleRateHz)
                    ?.let(::midiNumberOf)
            }
        }
        val frameNotes = medianSmoothed(rawFrameNotes)
        val minimumFrames = (minimumNoteMillis * sampleRateHz / MILLIS_PER_SECOND.toDouble() / hopSamples).roundToInt()
        val notes = mutableListOf<NoteEvent>()
        var runStart = 0
        while (runStart < frameNotes.size) {
            var runEnd = runStart
            while (runEnd + 1 < frameNotes.size && frameNotes[runEnd + 1] == frameNotes[runStart]) runEnd++
            val runMidiNumber = frameNotes[runStart]
            if (runMidiNumber != null && runEnd - runStart + 1 >= minimumFrames) {
                val onsetSample = runStart * hopSamples + windowSamples / 2
                notes += NoteEvent(
                    midiNumber = runMidiNumber,
                    onsetMillis = millisAt(onsetSample, sampleRateHz),
                    durationMillis = millisAt((runEnd - runStart + 1) * hopSamples, sampleRateHz)
                )
            }
            runStart = runEnd + 1
        }
        return notes
    }

    /**
     * @param frequencyHz Detected pitch. Type: Double
     * @return Nearest MIDI note number. Type: Int
     */
    private fun midiNumberOf(frequencyHz: Double): Int =
        A4_MIDI_NUMBER + (centsBetween(A4_FREQUENCY_HZ, frequencyHz) / CENTS_PER_SEMITONE).roundToInt()

    /**
     * Replaces each frame's note with the most typical note of its neighbours,
     * removing one- and two-frame glitches at note boundaries.
     *
     * @param frameNotes Note per frame, null for silence. Type: List<Int?>
     * @return Smoothed notes per frame. Type: List<Int?>
     */
    private fun medianSmoothed(frameNotes: List<Int?>): List<Int?> = frameNotes.indices.map { frame ->
        val neighbourhood = frameNotes.subList(
            maxOf(0, frame - SMOOTHING_RADIUS_FRAMES), minOf(frameNotes.size, frame + SMOOTHING_RADIUS_FRAMES + 1)
        ).map { it ?: SILENT_FRAME }.sorted()
        neighbourhood[neighbourhood.size / 2].takeIf { it != SILENT_FRAME }
    }

    /**
     * @param samples Audio. Type: FloatArray
     * @param start First sample of the region. Type: Int
     * @param length Region length. Type: Int
     * @return Root-mean-square level of the region. Type: Double
     */
    private fun rootMeanSquare(samples: FloatArray, start: Int, length: Int): Double {
        val clampedStart = start.coerceIn(0, samples.size)
        val clampedEnd = (start + length).coerceIn(clampedStart, samples.size)
        if (clampedEnd == clampedStart) return 0.0
        var sumOfSquares = 0.0
        for (index in clampedStart until clampedEnd) sumOfSquares += samples[index] * samples[index].toDouble()
        return sqrt(sumOfSquares / (clampedEnd - clampedStart))
    }

    /**
     * @param sampleCount A number of samples. Type: Int
     * @param sampleRateHz Sample rate. Type: Int
     * @return That many samples in milliseconds. Type: Long
     */
    private fun millisAt(sampleCount: Int, sampleRateHz: Int): Long =
        (sampleCount * MILLIS_PER_SECOND.toDouble() / sampleRateHz).roundToLong()

    private companion object {
        const val WINDOW_MILLIS = 93
        const val HOP_MILLIS = 12
        const val MILLIS_PER_SECOND = 1_000
        const val SMOOTHING_RADIUS_FRAMES = 2
        const val SILENT_FRAME = -1
        const val DEFAULT_SILENCE_THRESHOLD = 0.02
        const val DEFAULT_MINIMUM_NOTE_MILLIS = 60L
        const val A4_MIDI_NUMBER = 69
        const val A4_FREQUENCY_HZ = 440.0
        const val CENTS_PER_SEMITONE = 100.0
    }
}
