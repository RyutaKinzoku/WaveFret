package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.practice.TabNote
import com.example.wavefret.practice.TabTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SongImporterTest {

    private val songImporter = SongImporter(
        asciiTabParser = AsciiTabParser(BassTunings.STANDARD),
        tabTextParser = TabTextParser(BassTunings.STANDARD)
    )
    private val asciiTab = "G|-----|\nD|-----|\nA|--2--|\nE|--0--|"

    @Test
    fun importsAnAsciiTabWithTheGivenTitleAndTempo() {
        val exercise = songImporter.import(pastedText = asciiTab, title = "My riff", tempoBpm = 100)

        assertEquals("My riff", exercise.title)
        assertEquals(100, exercise.tempoBpm)
        assertEquals(listOf(TabNote(0, 0), TabNote(1, 2)), exercise.notes)
    }

    @Test
    fun importsWaveFretTabTextKeepingItsOwnTitleWhenNoneIsGiven() {
        val exercise = songImporter.import(pastedText = "title: Own format\ntempo: 70\nE0 A0", title = " ", tempoBpm = null)

        assertEquals("Own format", exercise.title)
        assertEquals(70, exercise.tempoBpm)
    }

    @Test
    fun givenTitleAndTempoOverrideThoseInWaveFretTabText() {
        val exercise = songImporter.import(pastedText = "title: Old\ntempo: 70\nE0", title = "New", tempoBpm = 110)

        assertEquals("New", exercise.title)
        assertEquals(110, exercise.tempoBpm)
    }

    @Test
    fun asciiTabWithoutTitleGetsTheDefaultImportedTitle() {
        val exercise = songImporter.import(pastedText = asciiTab, title = "", tempoBpm = null)

        assertEquals(SongImporter.DEFAULT_IMPORTED_TITLE, exercise.title)
        assertEquals(TabTextParser.DEFAULT_TEMPO_BPM, exercise.tempoBpm)
    }

    @Test
    fun textInNeitherFormatIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            songImporter.import(pastedText = "Am G C F, sing along", title = "Song", tempoBpm = null)
        }
    }
}
