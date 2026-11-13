package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.music.BassTuning
import com.example.wavefret.practice.songimport.AsciiTabParser
import com.example.wavefret.practice.songimport.AsciiTabWriter
import com.example.wavefret.practice.songimport.FretboardPositionFinder

/**
 * Combines what was read from every page into one song: drawn tab staffs
 * first, then sheet music placed on the fretboard, then a tab written as
 * text, in that order of reliability.
 *
 * @property tuning Tuning of the bass. Type: BassTuning
 */
class ScannedSongAssembler(tuning: BassTuning) {

    private val asciiTabWriter = AsciiTabWriter(tuning)
    private val asciiTabParser = AsciiTabParser(tuning)
    private val notationTabArranger = NotationTabArranger(FretboardPositionFinder(tuning))

    /**
     * @param pages What was read from each page, in order. Type: List<PageReading>
     * @param textTabFallback Text recognized on the pages, used when no staff was found. Type: String
     * @return The song. Type: ScannedSong
     */
    fun assemble(pages: List<PageReading>, textTabFallback: String): ScannedSong {
        val tabSystems = pages.flatMap { it.tabSystems }.filter { it.isNotEmpty() }
        if (tabSystems.isNotEmpty()) {
            return ScannedSong(ScannedSong.Source.DRAWN_TAB, asciiTabWriter.write(tabSystems), textTabFallback)
        }
        val arrangedSystems = notationTabArranger.arrange(pages.flatMap { it.notationSystems }).filter { it.isNotEmpty() }
        if (arrangedSystems.isNotEmpty()) {
            return ScannedSong(ScannedSong.Source.SHEET_MUSIC, asciiTabWriter.write(arrangedSystems), textTabFallback)
        }
        val textTab = asciiTabParser.extractTab(textTabFallback)
        if (textTab != null && runCatching { asciiTabParser.parseNotes(textTab) }.isSuccess) {
            return ScannedSong(ScannedSong.Source.TEXT_TAB, textTab, textTabFallback)
        }
        return ScannedSong(ScannedSong.Source.NOTHING, null, textTabFallback)
    }
}
