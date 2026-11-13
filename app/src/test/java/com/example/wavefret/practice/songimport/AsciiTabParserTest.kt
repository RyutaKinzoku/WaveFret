package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AsciiTabParserTest {

    private val asciiTabParser = AsciiTabParser(BassTunings.STANDARD)

    @Test
    fun readsNotesLeftToRightAcrossStrings() {
        val asciiTab = """
            G|-------------|
            D|-------------|
            A|-----0--2--3-|
            E|--3----------|
        """.trimIndent()

        val notes = asciiTabParser.parseNotes(asciiTab)

        assertEquals(listOf(TabNote(0, 3), TabNote(1, 0), TabNote(1, 2), TabNote(1, 3)), notes)
    }

    @Test
    fun readsMultiDigitFretsAsOneNote() {
        val asciiTab = """
            G|--12--|
            D|------|
            A|------|
            E|------|
        """.trimIndent()

        assertEquals(listOf(TabNote(3, 12)), asciiTabParser.parseNotes(asciiTab))
    }

    @Test
    fun simultaneousNotesAreOrderedLowStringFirst() {
        val asciiTab = """
            G|-----|
            D|--2--|
            A|--0--|
            E|-----|
        """.trimIndent()

        assertEquals(listOf(TabNote(1, 0), TabNote(2, 2)), asciiTabParser.parseNotes(asciiTab))
    }

    @Test
    fun joinsSeveralSystemsInOrderAndIgnoresSurroundingText() {
        val asciiTab = """
            Verse riff (x2)
            G|-----|
            D|-----|
            A|-----|
            E|--0--|

            Lyrics go here
            G|-----|
            D|-----|
            A|--5--|
            E|-----|
        """.trimIndent()

        assertEquals(listOf(TabNote(0, 0), TabNote(1, 5)), asciiTabParser.parseNotes(asciiTab))
    }

    @Test
    fun acceptsLowercaseNamesAndTechniqueMarks() {
        val asciiTab = """
            g|---------|
            d|---------|
            a|---------|
            e|--3h5-7~-|
        """.trimIndent()

        assertEquals(listOf(TabNote(0, 3), TabNote(0, 5), TabNote(0, 7)), asciiTabParser.parseNotes(asciiTab))
    }

    @Test
    fun acceptsLinesWithoutTheBarAfterTheStringName() {
        val asciiTab = """
            G-------
            D-------
            A---2---
            E-------
        """.trimIndent()

        assertEquals(listOf(TabNote(1, 2)), asciiTabParser.parseNotes(asciiTab))
    }

    @Test
    fun textWithoutAnyTabSystemIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            asciiTabParser.parseNotes("Just some lyrics\nand chords: Am G C")
        }
    }

    @Test
    fun systemWithoutAnyNotesIsRejected() {
        val emptyTab = "G|----|\nD|----|\nA|----|\nE|----|"

        assertThrows(IllegalArgumentException::class.java) { asciiTabParser.parseNotes(emptyTab) }
    }

    @Test
    fun treatsTypographicDashesAndNonBreakingSpacesAsPlainTab() {
        val asciiTab = "G|\u2013\u2013\u2013\u2013\u2013|\nD|\u2014\u2014\u2014\u2014\u2014|\nA|\u2013\u20135\u2013\u2013|\nE|\u00A03\u2013\u2013\u2013\u2013|"

        assertEquals(listOf(TabNote(0, 3), TabNote(1, 5)), asciiTabParser.parseNotes(asciiTab))
    }

    @Test
    fun ignoresRepeatCountsAfterTheLastBar() {
        val asciiTab = """
            G|---------|
            D|---------|
            A|---------|  x4
            E|--3--5---|  (x2)
        """.trimIndent()

        assertEquals(listOf(TabNote(0, 3), TabNote(0, 5)), asciiTabParser.parseNotes(asciiTab))
    }

    @Test
    fun readsSystemsWhoseLinesHaveNoStringNames() {
        val asciiTab = """
            |---------|
            |------2--|
            |---------|
            |--3------|
        """.trimIndent()

        assertEquals(listOf(TabNote(0, 3), TabNote(2, 2)), asciiTabParser.parseNotes(asciiTab))
    }

    @Test
    fun skipsDeadAndGhostNoteMarksWithoutLosingTheLine() {
        val asciiTab = """
            G|----------|
            D|----------|
            A|--X--g5-v-|
            E|-3-----P--|
        """.trimIndent()

        assertEquals(listOf(TabNote(0, 3), TabNote(1, 5)), asciiTabParser.parseNotes(asciiTab))
    }

    @Test
    fun readsTheFourHighStringsOfAFiveStringTab() {
        val asciiTab = """
            G|--------|
            D|--------|
            A|-----2--|
            E|--0-----|
            B|-----0--|
        """.trimIndent()

        assertEquals(listOf(TabNote(0, 0), TabNote(1, 2)), asciiTabParser.parseNotes(asciiTab))
    }

    @Test
    fun readsTabLinesIndentedWithTabCharacters() {
        val asciiTab = "\tG|-----|\n\tD|-----|\n\tA|--7--|\n\tE|-----|"

        assertEquals(listOf(TabNote(1, 7)), asciiTabParser.parseNotes(asciiTab))
    }

    @Test
    fun ignoresChordAndBarCountLinesBetweenSystems() {
        val asciiTab = """
            [Verse]
               Am          G
            |  1   |   2   |
            G|------|------|
            D|------|------|
            A|--0---|------|
            E|------|--3---|
        """.trimIndent()

        assertEquals(listOf(TabNote(1, 0), TabNote(0, 3)), asciiTabParser.parseNotes(asciiTab))
    }
}
