package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.textrecognition.RecognizedSymbol
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Test

class RecognizedTabTextBuilderTest {

    private val recognizedTabTextBuilder = RecognizedTabTextBuilder()

    /** Lays out [text] as one row of 10 px wide symbols at [top], skipping spaces like a recognizer does. */
    private fun row(text: String, top: Int): List<RecognizedSymbol> =
        text.mapIndexedNotNull { column, character ->
            if (character == ' ') null else RecognizedSymbol(character.toString(), column * 10, top, column * 10 + 10, top + 20)
        }

    private fun systemWithoutStaffLines(topOffset: Int): List<RecognizedSymbol> =
        row("G|        |", topOffset) +
            row("D|        |", topOffset + 24) +
            row("A|     2  |", topOffset + 48) +
            row("E|  3     |", topOffset + 72)

    @Test
    fun photographedTabWithoutVisibleStaffLinesBecomesReadableTab() {
        val text = recognizedTabTextBuilder.build(listOf(systemWithoutStaffLines(0)))

        assertEquals(listOf(TabNote(0, 3), TabNote(1, 2)), AsciiTabParser(BassTunings.STANDARD).parseNotes(text))
    }

    @Test
    fun pagesAreJoinedInOrder() {
        val firstPage = systemWithoutStaffLines(0)
        val secondPage = row("G|  9  |", 0) + row("D|     |", 24) + row("A|     |", 48) + row("E|     |", 72)

        val notes = AsciiTabParser(BassTunings.STANDARD).parseNotes(recognizedTabTextBuilder.build(listOf(firstPage, secondPage)))

        assertEquals(listOf(TabNote(0, 3), TabNote(1, 2), TabNote(3, 9)), notes)
    }

    @Test
    fun pageWithoutTextAddsNothing() {
        assertEquals("", recognizedTabTextBuilder.build(listOf(emptyList())))
    }
}
