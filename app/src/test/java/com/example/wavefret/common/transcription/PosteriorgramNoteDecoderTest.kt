package com.example.wavefret.common.transcription

import com.example.wavefret.common.music.NoteEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class PosteriorgramNoteDecoderTest {

    private val decoder = PosteriorgramNoteDecoder(
        onsetThreshold = 0.5f, frameThreshold = 0.3f, minimumNoteFrames = 3, gapToleranceFrames = 2, framesPerSecond = 100.0
    )

    /** frames x 88 matrix filled with zeros. */
    private fun emptyMap(frameCount: Int) = Array(frameCount) { FloatArray(88) }

    @Test
    fun anOnsetFollowedBySustainedFramesIsANote() {
        val onsets = emptyMap(20).also { it[2][7] = 0.9f }
        val frames = emptyMap(20).also { map -> (2..9).forEach { map[it][7] = 0.8f } }

        val notes = decoder.decode(frames, onsets)

        // bin 7 = MIDI 28 (low E); frames 2..9 at 100 fps = 20 ms .. 100 ms
        assertEquals(listOf(NoteEvent(28, 20, 80)), notes)
    }

    @Test
    fun shortBlipsAreIgnored() {
        val onsets = emptyMap(20).also { it[2][7] = 0.9f }
        val frames = emptyMap(20).also { map -> (2..3).forEach { map[it][7] = 0.8f } }

        assertEquals(emptyList<NoteEvent>(), decoder.decode(frames, onsets))
    }

    @Test
    fun briefDropsInsideANoteDoNotSplitIt() {
        val onsets = emptyMap(30).also { it[0][12] = 0.9f }
        val frames = emptyMap(30).also { map ->
            (0..5).forEach { map[it][12] = 0.8f }
            (8..12).forEach { map[it][12] = 0.8f }
        }

        assertEquals(listOf(NoteEvent(33, 0, 130)), decoder.decode(frames, onsets))
    }

    @Test
    fun aSecondOnsetStartsANewNoteOnTheSamePitch() {
        val onsets = emptyMap(30).also { it[0][12] = 0.9f; it[10][12] = 0.9f }
        val frames = emptyMap(30).also { map -> (0..19).forEach { map[it][12] = 0.8f } }

        assertEquals(listOf(0L, 100L), decoder.decode(frames, onsets).map { it.onsetMillis })
    }

    @Test
    fun onlyLocalOnsetPeaksCount() {
        val onsets = emptyMap(20).also { it[3][7] = 0.6f; it[4][7] = 0.9f; it[5][7] = 0.7f }
        val frames = emptyMap(20).also { map -> (3..12).forEach { map[it][7] = 0.8f } }

        assertEquals(listOf(40L), decoder.decode(frames, onsets).map { it.onsetMillis })
    }

    @Test
    fun notesAreReturnedInTimeOrder() {
        val onsets = emptyMap(40).also { it[20][7] = 0.9f; it[0][12] = 0.9f }
        val frames = emptyMap(40).also { map ->
            (0..6).forEach { map[it][12] = 0.8f }
            (20..26).forEach { map[it][7] = 0.8f }
        }

        assertEquals(listOf(33, 28), decoder.decode(frames, onsets).map { it.midiNumber })
    }
}
