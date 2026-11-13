package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.music.NoteEvent
import com.example.wavefret.practice.LessonMediaSource
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoTabLessonBuilderTest {

    private val videoTabLessonBuilder = VideoTabLessonBuilder(BassTunings.STANDARD)
    private val video = LessonMediaSource.YouTubeVideo("dQw4w9WgXcQ")

    private fun collectorWith(vararg columns: List<TabNote>): VideoTabCollector =
        VideoTabCollector().apply { onFrame(columns.toList(), frameTimeMillis = 0) }

    @Test
    fun theTabInTheVideoGivesTheNotesAndTheSoundGivesTheTiming() {
        val collector = collectorWith(listOf(TabNote(0, 0)), listOf(TabNote(0, 3)), listOf(TabNote(1, 0)))
        val heard = listOf(NoteEvent(28, 1_000, 200), NoteEvent(31, 1_400, 200), NoteEvent(45, 1_600, 200), NoteEvent(33, 1_800, 200))

        val lesson = videoTabLessonBuilder.build("Riff", collector, heard, video)!!

        assertEquals(listOf(TabNote(0, 0), TabNote(0, 3), TabNote(1, 0)), lesson.notes)
        assertEquals(listOf(1_000L, 1_400L, 1_800L), lesson.noteOnsetsMillis)
        assertEquals(video, lesson.mediaSource)
    }

    @Test
    fun withoutMatchingSoundTheTimesTheTabAppearedAreUsed() {
        val collector = collectorWith(listOf(TabNote(0, 0)), listOf(TabNote(0, 3)), listOf(TabNote(1, 0)))

        val lesson = videoTabLessonBuilder.build("Riff", collector, emptyList(), video)!!

        assertEquals(listOf(0L, 500L, 1_000L), lesson.noteOnsetsMillis)
    }

    @Test
    fun aChordColumnGivesItsLowestNote() {
        val collector = collectorWith(listOf(TabNote(0, 5), TabNote(2, 7)), listOf(TabNote(1, 2)), listOf(TabNote(1, 3)))

        assertEquals(
            listOf(TabNote(0, 5), TabNote(1, 2), TabNote(1, 3)),
            videoTabLessonBuilder.build("Riff", collector, emptyList(), video)!!.notes
        )
    }

    @Test
    fun tooLittleTabGivesNoLesson() {
        assertNull(videoTabLessonBuilder.build("Riff", collectorWith(listOf(TabNote(0, 0))), emptyList(), video))
    }
}
