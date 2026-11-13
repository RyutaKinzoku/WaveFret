package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Test

class AsciiTabWriterTest {

    private val asciiTabWriter = AsciiTabWriter(BassTunings.STANDARD)

    @Test
    fun writesEachColumnAcrossTheStrings() {
        val system = listOf(listOf(TabNote(0, 3)), listOf(TabNote(1, 12)), listOf(TabNote(0, 0), TabNote(2, 2)))

        val expected = """
            G|----------|
            D|-------2--|
            A|----12----|
            E|--3----0--|
        """.trimIndent()
        assertEquals(expected, asciiTabWriter.write(listOf(system)))
    }

    @Test
    fun separatesSystemsWithABlankLine() {
        val systems = listOf(listOf(listOf(TabNote(0, 1))), listOf(listOf(TabNote(3, 2))))

        assertEquals("G|-----|\nD|-----|\nA|-----|\nE|--1--|\n\nG|--2--|\nD|-----|\nA|-----|\nE|-----|", asciiTabWriter.write(systems))
    }

    @Test
    fun aShortSystemStillReadsBack() {
        val systems = listOf(listOf(listOf(TabNote(1, 2))))

        assertEquals(listOf(TabNote(1, 2)), AsciiTabParser(BassTunings.STANDARD).parseNotes(asciiTabWriter.write(systems)))
    }

    @Test
    fun whatItWritesReadsBackAsTheSameNotes() {
        val system = listOf(listOf(TabNote(0, 5)), listOf(TabNote(0, 3)), listOf(TabNote(1, 7)), listOf(TabNote(3, 10)))

        val notes = AsciiTabParser(BassTunings.STANDARD).parseNotes(asciiTabWriter.write(listOf(system)))

        assertEquals(system.flatten(), notes)
    }
}
