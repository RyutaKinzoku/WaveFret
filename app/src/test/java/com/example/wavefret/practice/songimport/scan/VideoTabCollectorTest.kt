package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.practice.TabNote
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoTabCollectorTest {

    private val videoTabCollector = VideoTabCollector()

    /** Columns of single notes on the E string with the given frets. */
    private fun frame(vararg frets: Int): List<List<TabNote>> = frets.map { listOf(TabNote(0, it)) }

    private fun collectedFrets(): List<Int> = videoTabCollector.columns.map { it.single().fret }

    @Test
    fun theFirstFrameIsTakenWhole() {
        videoTabCollector.onFrame(frame(0, 0, 3, 0, 5))

        assertEquals(listOf(0, 0, 3, 0, 5), collectedFrets())
    }

    @Test
    fun theSamePageSeenAgainAddsNothing() {
        videoTabCollector.onFrame(frame(0, 0, 3, 0, 5))
        videoTabCollector.onFrame(frame(0, 0, 3, 0, 5))

        assertEquals(listOf(0, 0, 3, 0, 5), collectedFrets())
    }

    @Test
    fun aScrolledViewAddsOnlyItsNewNotes() {
        videoTabCollector.onFrame(frame(1, 2, 3, 4, 5, 6))
        videoTabCollector.onFrame(frame(4, 5, 6, 7, 8))

        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8), collectedFrets())
    }

    @Test
    fun aNewPageIsAddedWhole() {
        videoTabCollector.onFrame(frame(1, 2, 3, 4))
        videoTabCollector.onFrame(frame(7, 7, 9, 10))

        assertEquals(listOf(1, 2, 3, 4, 7, 7, 9, 10), collectedFrets())
    }

    @Test
    fun aMisreadNumberDoesNotAddThePageAgain() {
        videoTabCollector.onFrame(frame(0, 0, 3, 0, 5, 5, 3, 0))
        videoTabCollector.onFrame(frame(0, 0, 3, 0, 6, 5, 3, 0))

        assertEquals(listOf(0, 0, 3, 0, 5, 5, 3, 0), collectedFrets())
    }

    @Test
    fun framesWithoutTabAreIgnored() {
        videoTabCollector.onFrame(frame(1, 2, 3))
        videoTabCollector.onFrame(emptyList())
        videoTabCollector.onFrame(frame(1, 2, 3))

        assertEquals(listOf(1, 2, 3), collectedFrets())
    }

    @Test
    fun aHalfReadPageIsCompletedByTheNextFrame() {
        videoTabCollector.onFrame(frame(9, 9))
        videoTabCollector.onFrame(frame(1, 2, 3))
        videoTabCollector.onFrame(frame(1, 2, 3, 4, 5, 6))

        assertEquals(listOf(9, 9, 1, 2, 3, 4, 5, 6), collectedFrets())
    }
}
