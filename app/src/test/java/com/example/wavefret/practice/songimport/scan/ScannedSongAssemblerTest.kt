package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.practice.TabNote
import com.example.wavefret.practice.songimport.AsciiTabParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScannedSongAssemblerTest {

    private val scannedSongAssembler = ScannedSongAssembler(BassTunings.STANDARD)
    private val asciiTabParser = AsciiTabParser(BassTunings.STANDARD)

    @Test
    fun drawnTabsFromEveryPageBecomeOneTab() {
        val pages = listOf(
            PageReading(listOf(listOf(listOf(TabNote(0, 5)), listOf(TabNote(0, 3)))), emptyList()),
            PageReading(listOf(listOf(listOf(TabNote(1, 2)))), emptyList())
        )

        val scannedSong = scannedSongAssembler.assemble(pages, textTabFallback = "")

        assertEquals(ScannedSong.Source.DRAWN_TAB, scannedSong.source)
        assertEquals(listOf(TabNote(0, 5), TabNote(0, 3), TabNote(1, 2)), asciiTabParser.parseNotes(scannedSong.tabText!!))
    }

    @Test
    fun sheetMusicIsPlacedOnTheFretboardWhenThereIsNoTab() {
        val pages = listOf(PageReading(emptyList(), listOf(listOf(28, 33))))

        val scannedSong = scannedSongAssembler.assemble(pages, textTabFallback = "")

        assertEquals(ScannedSong.Source.SHEET_MUSIC, scannedSong.source)
        assertEquals(listOf(TabNote(0, 0), TabNote(0, 5)), asciiTabParser.parseNotes(scannedSong.tabText!!))
    }

    @Test
    fun aTextTabIsUsedWhenNoStaffWasFound() {
        val textTab = "G|----|\nD|----|\nA|-2--|\nE|----|"

        val scannedSong = scannedSongAssembler.assemble(listOf(PageReading(emptyList(), emptyList())), textTab)

        assertEquals(ScannedSong.Source.TEXT_TAB, scannedSong.source)
        assertEquals(textTab, scannedSong.tabText)
    }

    @Test
    fun nothingReadableGivesNoTab() {
        val scannedSong = scannedSongAssembler.assemble(listOf(PageReading(emptyList(), emptyList())), "Just a title")

        assertEquals(ScannedSong.Source.NOTHING, scannedSong.source)
        assertNull(scannedSong.tabText)
        assertEquals("Just a title", scannedSong.recognizedText)
    }
}
