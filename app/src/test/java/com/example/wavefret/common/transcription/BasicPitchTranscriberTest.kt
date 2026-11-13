package com.example.wavefret.common.transcription

import com.example.wavefret.common.audio.AudioResampler
import org.junit.Assert.assertEquals
import org.junit.Test

class BasicPitchTranscriberTest {

    /** Fake model: every window has low E (bin 7) starting at frame 20 and lasting 40 frames. */
    private class FakeBasicPitchModel : BasicPitchModel {
        var windowsSeen = 0
        override fun predict(window: FloatArray): BasicPitchPrediction {
            windowsSeen++
            val notes = Array(BasicPitchWindowing.FRAMES_PER_WINDOW) { FloatArray(88) }
            val onsets = Array(BasicPitchWindowing.FRAMES_PER_WINDOW) { FloatArray(88) }
            onsets[20][7] = 0.9f
            (20 until 60).forEach { notes[it][7] = 0.8f }
            return BasicPitchPrediction(firstMap = onsets, secondMap = notes)
        }
    }

    @Test
    fun runsTheModelOnEveryWindowAndDecodesNotes() {
        val fakeModel = FakeBasicPitchModel()
        val transcriber = BasicPitchTranscriber(fakeModel, BasicPitchWindowing(), PosteriorgramNoteDecoder(), AudioResampler())
        val oneSecondAt44k = FloatArray(44_100)

        val notes = transcriber.transcribe(oneSecondAt44k, 44_100)

        assertEquals(1, fakeModel.windowsSeen)
        // frame 20 of the first window is frame 5 of the audio after dropping 15 overlap frames: ~58 ms
        assertEquals(listOf(28), notes.map { it.midiNumber })
        assertEquals(58L, notes.single().onsetMillis)
    }

    @Test
    fun keepsOnlyNotesInTheBassRange() {
        val transcriber = BasicPitchTranscriber(
            object : BasicPitchModel {
                override fun predict(window: FloatArray): BasicPitchPrediction {
                    val notes = Array(BasicPitchWindowing.FRAMES_PER_WINDOW) { FloatArray(88) }
                    val onsets = Array(BasicPitchWindowing.FRAMES_PER_WINDOW) { FloatArray(88) }
                    listOf(7, 60).forEach { bin -> onsets[20][bin] = 0.9f; (20 until 60).forEach { notes[it][bin] = 0.8f } }
                    return BasicPitchPrediction(onsets, notes)
                }
            },
            BasicPitchWindowing(), PosteriorgramNoteDecoder(), AudioResampler()
        )

        assertEquals(listOf(28), transcriber.transcribe(FloatArray(22_050), 22_050).map { it.midiNumber })
    }

    @Test
    fun keepsOnlyTheBassUnderAChordInTheBassRange() {
        val transcriber = BasicPitchTranscriber(
            object : BasicPitchModel {
                override fun predict(window: FloatArray): BasicPitchPrediction {
                    val notes = Array(BasicPitchWindowing.FRAMES_PER_WINDOW) { FloatArray(88) }
                    val onsets = Array(BasicPitchWindowing.FRAMES_PER_WINDOW) { FloatArray(88) }
                    listOf(12, 19, 24).forEach { bin -> onsets[20][bin] = 0.9f; (20 until 60).forEach { notes[it][bin] = 0.8f } }
                    return BasicPitchPrediction(onsets, notes)
                }
            },
            BasicPitchWindowing(), PosteriorgramNoteDecoder(), AudioResampler()
        )

        assertEquals(listOf(33), transcriber.transcribe(FloatArray(22_050), 22_050).map { it.midiNumber })
    }
}
