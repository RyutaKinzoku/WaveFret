package com.example.wavefret.practice

import com.example.wavefret.common.music.BassTunings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TabTextParserTest {

    private val tabTextParser = TabTextParser(BassTunings.STANDARD)

    @Test
    fun parsesTitleTempoAndNotes() {
        val tabText = """
            title: Open strings
            tempo: 72
            E0 A0 D0 G0
        """.trimIndent()

        val exercise = tabTextParser.parse(tabText)

        assertEquals("Open strings", exercise.title)
        assertEquals(72, exercise.tempoBpm)
        assertEquals(listOf(TabNote(0, 0), TabNote(1, 0), TabNote(2, 0), TabNote(3, 0)), exercise.notes)
    }

    @Test
    fun parsesMultiDigitFretsAndJoinsNoteLinesInOrder() {
        val tabText = """
            title: High frets
            E12 A10
            G3
        """.trimIndent()

        val exercise = tabTextParser.parse(tabText)

        assertEquals(listOf(TabNote(0, 12), TabNote(1, 10), TabNote(3, 3)), exercise.notes)
    }

    @Test
    fun stringNamesAreCaseInsensitive() {
        val exercise = tabTextParser.parse("title: Lower\ne3 g5")

        assertEquals(listOf(TabNote(0, 3), TabNote(3, 5)), exercise.notes)
    }

    @Test
    fun ignoresCommentsAndBlankLines() {
        val tabText = """
            # a warm-up
            title: Commented

            E0  # first note
        """.trimIndent()

        val exercise = tabTextParser.parse(tabText)

        assertEquals(listOf(TabNote(0, 0)), exercise.notes)
    }

    @Test
    fun usesDefaultTempoWhenNoneIsGiven() {
        val exercise = tabTextParser.parse("title: No tempo\nE0")

        assertEquals(TabTextParser.DEFAULT_TEMPO_BPM, exercise.tempoBpm)
    }

    @Test
    fun usesUntitledWhenNoTitleIsGiven() {
        val exercise = tabTextParser.parse("E0")

        assertEquals(TabTextParser.UNTITLED_EXERCISE_TITLE, exercise.title)
    }

    @Test
    fun unknownStringNameReportsTheLineNumber() {
        val parseFailure = assertThrows(TabParseException::class.java) {
            tabTextParser.parse("title: Bad\nE0\nX3")
        }

        assertEquals(3, parseFailure.lineNumber)
    }

    @Test
    fun tokenWithoutFretIsRejected() {
        assertThrows(TabParseException::class.java) { tabTextParser.parse("E") }
    }

    @Test
    fun invalidTempoIsRejected() {
        assertThrows(TabParseException::class.java) { tabTextParser.parse("tempo: fast\nE0") }
    }

    @Test
    fun tabWithoutAnyNotesIsRejected() {
        assertThrows(TabParseException::class.java) { tabTextParser.parse("title: Nothing") }
    }

    @Test
    fun parsesNoteTimingsInMilliseconds() {
        val exercise = tabTextParser.parse("title: Timed\nE0 A2\ntiming: 0 750")

        assertEquals(listOf(0L, 750L), exercise.noteOnsetsMillis)
    }

    @Test
    fun timingCountThatDoesNotMatchTheNotesIsRejected() {
        assertThrows(TabParseException::class.java) { tabTextParser.parse("E0 A2\ntiming: 0") }
    }

    @Test
    fun tabWithoutTimingHasNoTimings() {
        assertEquals(null, tabTextParser.parse("E0").noteOnsetsMillis)
    }

    @Test
    fun parsesAYouTubeSource() {
        val exercise = tabTextParser.parse("E0\ntiming: 0\nsource: youtube dQw4w9WgXcQ")

        assertEquals(LessonMediaSource.YouTubeVideo("dQw4w9WgXcQ"), exercise.mediaSource)
    }

    @Test
    fun parsesAStoredAudioSource() {
        val exercise = tabTextParser.parse("E0\ntiming: 0\nsource: audio 1700000000000.mp3")

        assertEquals(LessonMediaSource.StoredAudio("1700000000000.mp3"), exercise.mediaSource)
    }

    @Test
    fun unknownSourceKindIsRejected() {
        assertThrows(TabParseException::class.java) { tabTextParser.parse("E0\nsource: vinyl x") }
    }
}
