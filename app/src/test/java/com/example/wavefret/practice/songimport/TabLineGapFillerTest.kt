package com.example.wavefret.practice.songimport

import org.junit.Assert.assertEquals
import org.junit.Test

class TabLineGapFillerTest {

    private val tabLineGapFiller = TabLineGapFiller()

    @Test
    fun fillsGapsInsideATabLineWithDashes() {
        assertEquals("G|--3---5--|", tabLineGapFiller.fill("G|  3   5  |"))
    }

    @Test
    fun fillsGapsInATabLineWithoutAStringName() {
        assertEquals("|--3--|", tabLineGapFiller.fill("|  3  |"))
    }

    @Test
    fun fillsGapsInATabLineWithoutABar() {
        assertEquals("E --3--5--", tabLineGapFiller.fill("E -- 3  5 --"))
    }

    @Test
    fun keepsTrailingSpacesOutOfTheTab() {
        assertEquals("A|--2--|", tabLineGapFiller.fill("A|  2  |   "))
    }

    @Test
    fun leavesLyricsChordsAndTitlesAlone() {
        val text = "Get up offa that thing\n   Am      G\nBass tab by someone"

        assertEquals(text, tabLineGapFiller.fill(text))
    }

    @Test
    fun fillsEveryTabLineOfAMultiLineText() {
        val text = "Intro\nG|  5 |\nD| 7  |"

        assertEquals("Intro\nG|--5-|\nD|-7--|", tabLineGapFiller.fill(text))
    }
}
