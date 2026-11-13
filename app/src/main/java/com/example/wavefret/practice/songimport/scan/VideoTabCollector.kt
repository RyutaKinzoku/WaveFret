package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.practice.TabNote

/**
 * Builds one tab from the frames of a video lesson. Each frame shows part
 * of the tab; a frame that shows the same notes as the previous one adds
 * nothing, a view that scrolled on adds only the notes after the overlap,
 * and a new page adds all its notes. A few misread numbers don't break
 * the overlap.
 *
 * @property minimumOverlap Columns that must match to count as an overlap. Type: Int
 * @property allowedMismatchShare Share of overlapping columns that may differ (misreads). Type: Double
 */
class VideoTabCollector(
    private val minimumOverlap: Int = DEFAULT_MINIMUM_OVERLAP,
    private val allowedMismatchShare: Double = DEFAULT_ALLOWED_MISMATCH_SHARE
) {

    private val collectedColumns = mutableListOf<List<TabNote>>()
    private var previousFrame: List<List<TabNote>> = emptyList()

    /** Every column collected so far, in playing order. Type: List<List<TabNote>> */
    val columns: List<List<TabNote>> get() = collectedColumns.toList()

    /**
     * @param frameColumns Columns read from one frame, left to right and top to bottom. Type: List<List<TabNote>>
     * @return Unit
     */
    fun onFrame(frameColumns: List<List<TabNote>>) {
        if (frameColumns.isEmpty()) return
        val overlap = overlapLength(previousFrame, frameColumns)
        collectedColumns += frameColumns.drop(overlap)
        previousFrame = frameColumns
    }

    /**
     * @param previous Columns of the previous frame. Type: List<List<TabNote>>
     * @param current Columns of this frame. Type: List<List<TabNote>>
     * @return How many leading columns of this frame repeat the end of the previous one. Type: Int
     */
    private fun overlapLength(previous: List<List<TabNote>>, current: List<List<TabNote>>): Int {
        val longest = minOf(previous.size, current.size)
        val shortest = minOf(minimumOverlap, longest)
        for (length in longest downTo maxOf(shortest, 1)) {
            val previousEnd = previous.subList(previous.size - length, previous.size)
            val currentStart = current.subList(0, length)
            val mismatches = previousEnd.zip(currentStart).count { (earlier, later) -> earlier != later }
            if (mismatches <= (length * allowedMismatchShare).toInt()) return length
        }
        return 0
    }

    private companion object {
        const val DEFAULT_MINIMUM_OVERLAP = 3
        const val DEFAULT_ALLOWED_MISMATCH_SHARE = 0.15
    }
}
