package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.BinaryImage
import kotlin.math.abs

/**
 * Finds music and tab staffs on a page image: rows covered by long
 * horizontal runs of ink are staff lines (even when tab numbers break
 * them), and runs of 4 to 6 evenly spaced lines are staffs. Short lines,
 * bar lines and text are ignored.
 *
 * @property minimumCoverage Share of the page width a staff line's long runs must cover. Type: Double
 * @property spacingTolerance Allowed difference between a gap and the staff's average gap so far, as a share of it. Type: Double
 */
class StaffFinder(
    private val minimumCoverage: Double = DEFAULT_MINIMUM_COVERAGE,
    private val spacingTolerance: Double = DEFAULT_SPACING_TOLERANCE
) {

    /**
     * @param image Page image. Type: BinaryImage
     * @return Staffs top to bottom. Type: List<Staff>
     */
    fun find(image: BinaryImage): List<Staff> {
        val minimumRunLength = maxOf(MINIMUM_RUN_PIXELS, image.width / RUN_LENGTH_DIVISOR)
        val longRunsByRow = List(image.height) { y -> longRuns(image, y, minimumRunLength) }
        val coverageByRow = longRunsByRow.map { runs -> runs.sumOf { it.last - it.first + 1 } }
        val lines = mergeRowsIntoLines(coverageByRow.map { it >= image.width * minimumCoverage }, longRunsByRow)
        return groupIntoStaffs(lines)
    }

    /**
     * @param image Page image. Type: BinaryImage
     * @param y Row. Type: Int
     * @param minimumRunLength Shortest run kept. Type: Int
     * @return Horizontal ink runs of at least the minimum length. Type: List<IntRange>
     */
    private fun longRuns(image: BinaryImage, y: Int, minimumRunLength: Int): List<IntRange> {
        val runs = mutableListOf<IntRange>()
        var runStart = -1
        for (x in 0..image.width) {
            val isInk = x < image.width && image.isInk(x, y)
            if (isInk && runStart < 0) runStart = x
            if (!isInk && runStart >= 0) {
                if (x - runStart >= minimumRunLength) runs += runStart until x
                runStart = -1
            }
        }
        return runs
    }

    /**
     * @param isLineRow Whether each row qualifies as part of a staff line. Type: List<Boolean>
     * @param longRunsByRow Long runs of each row. Type: List<List<IntRange>>
     * @return Lines found, top to bottom. Type: List<FoundLine>
     */
    private fun mergeRowsIntoLines(isLineRow: List<Boolean>, longRunsByRow: List<List<IntRange>>): List<FoundLine> {
        val lines = mutableListOf<FoundLine>()
        var y = 0
        while (y < isLineRow.size) {
            if (!isLineRow[y]) {
                y++
                continue
            }
            val firstRow = y
            while (y + 1 < isLineRow.size && isLineRow[y + 1]) y++
            val rows = firstRow..y
            lines += FoundLine(
                centreY = (firstRow + y) / 2.0 + CENTRE_OF_PIXEL,
                thickness = (y - firstRow + 1).toDouble(),
                left = rows.minOf { row -> longRunsByRow[row].first().first },
                right = rows.maxOf { row -> longRunsByRow[row].last().last }
            )
            y++
        }
        return lines
    }

    /**
     * @param lines Lines top to bottom. Type: List<FoundLine>
     * @return Runs of 4 to 6 evenly spaced lines. Type: List<Staff>
     */
    private fun groupIntoStaffs(lines: List<FoundLine>): List<Staff> {
        val staffs = mutableListOf<Staff>()
        var start = 0
        while (start + MINIMUM_STAFF_LINES - 1 < lines.size) {
            var end = start + 1
            while (end + 1 < lines.size && end - start + 1 < MAXIMUM_STAFF_LINES) {
                val averageSpacing = (lines[end].centreY - lines[start].centreY) / (end - start)
                if (abs(lines[end + 1].centreY - lines[end].centreY - averageSpacing) > averageSpacing * spacingTolerance) break
                end++
            }
            if (end - start + 1 >= MINIMUM_STAFF_LINES) {
                val staffLines = lines.subList(start, end + 1)
                staffs += Staff(
                    lineYs = staffLines.map { it.centreY },
                    lineThickness = staffLines.map { it.thickness }.average(),
                    left = staffLines.minOf { it.left },
                    right = staffLines.maxOf { it.right }
                )
                start = end + 1
            } else {
                start++
            }
        }
        return staffs
    }

    /**
     * One horizontal line found on the page.
     *
     * @property centreY Vertical centre. Type: Double
     * @property thickness Thickness in rows. Type: Double
     * @property left Leftmost column. Type: Int
     * @property right Rightmost column. Type: Int
     */
    private data class FoundLine(val centreY: Double, val thickness: Double, val left: Int, val right: Int)

    private companion object {
        const val DEFAULT_MINIMUM_COVERAGE = 0.3
        const val DEFAULT_SPACING_TOLERANCE = 0.25
        const val MINIMUM_RUN_PIXELS = 8
        const val RUN_LENGTH_DIVISOR = 50
        const val MINIMUM_STAFF_LINES = 4
        const val MAXIMUM_STAFF_LINES = 6
        const val CENTRE_OF_PIXEL = 0.5
    }
}
