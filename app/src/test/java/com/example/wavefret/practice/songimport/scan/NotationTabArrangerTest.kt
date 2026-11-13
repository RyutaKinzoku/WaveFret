package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.practice.TabNote
import com.example.wavefret.practice.songimport.FretboardPositionFinder
import org.junit.Assert.assertEquals
import org.junit.Test

class NotationTabArrangerTest {

    private val notationTabArranger = NotationTabArranger(FretboardPositionFinder(BassTunings.STANDARD))

    @Test
    fun turnsEachPitchIntoOneTabColumn() {
        val systems = notationTabArranger.arrange(listOf(listOf(28, 31, 33)))

        assertEquals(listOf(listOf(listOf(TabNote(0, 0)), listOf(TabNote(0, 3)), listOf(TabNote(1, 0)))), systems)
    }

    @Test
    fun keepsTheHandInPlaceAcrossSystems() {
        val systems = notationTabArranger.arrange(listOf(listOf(33), listOf(38)))

        assertEquals(listOf(listOf(listOf(TabNote(1, 0))), listOf(listOf(TabNote(2, 0)))), systems)
    }

    @Test
    fun skipsPitchesTheBassCannotPlay() {
        assertEquals(listOf(listOf(listOf(TabNote(0, 0)))), notationTabArranger.arrange(listOf(listOf(20, 28))))
    }
}
