package com.example.wavefret.practice.songimport

import com.example.wavefret.common.audio.SyntheticSignals
import com.example.wavefret.common.audio.YinPitchDetector
import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.music.NoteEvent
import com.example.wavefret.common.transcription.MonophonicTranscriber
import com.example.wavefret.common.transcription.NoteTranscriber
import com.example.wavefret.common.transcription.TempoEstimator
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AudioLessonBuilderTest {

    private val tuning = BassTunings.STANDARD
    private val lessonBuilder = NoteEventsLessonBuilder(tuning, FretboardPositionFinder(tuning))

    private fun builderWith(noteTranscriber: NoteTranscriber) = AudioLessonBuilder(noteTranscriber, TempoEstimator(), lessonBuilder)

    @Test
    fun transcribedNotesBecomeATimedLessonWithAnEstimatedTempo() {
        val fakeTranscriber = object : NoteTranscriber {
            override fun transcribe(samples: FloatArray, sampleRateHz: Int) =
                listOf(NoteEvent(28, 0, 500), NoteEvent(33, 600, 500), NoteEvent(38, 1_200, 500))
        }

        val lesson = builderWith(fakeTranscriber).build("My take", FloatArray(10), 22_050)

        assertEquals("My take", lesson.title)
        assertEquals(100, lesson.tempoBpm)
        assertEquals(listOf(0L, 600L, 1_200L), lesson.noteOnsetsMillis)
    }

    @Test
    fun audioWithoutDetectableNotesIsRejectedWithAReason() {
        val silentTranscriber = object : NoteTranscriber {
            override fun transcribe(samples: FloatArray, sampleRateHz: Int) = emptyList<NoteEvent>()
        }

        val failure = assertThrows(IllegalArgumentException::class.java) {
            builderWith(silentTranscriber).build("Nothing", FloatArray(10), 22_050)
        }
        assertEquals(AudioLessonBuilder.NO_NOTES_REASON, failure.message)
    }

    @Test
    fun aRealisticBassLineIsTranscribedEndToEnd() {
        val sampleRate = 22_050
        val bassLine = listOf(41.2, 55.0, 73.42).map { frequencyHz ->
            SyntheticSignals.bassLikeTone(frequencyHz, sampleRate, sampleRate / 2)
        }.reduce { all, note -> all + note }

        val lesson = builderWith(MonophonicTranscriber(YinPitchDetector())).build("Open strings", bassLine, sampleRate)

        assertEquals(listOf(TabNote(0, 0), TabNote(1, 0), TabNote(2, 0)), lesson.notes)
    }
}
