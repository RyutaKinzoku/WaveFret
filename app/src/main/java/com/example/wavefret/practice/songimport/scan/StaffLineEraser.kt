package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.BinaryImage
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Removes staff lines (and, optionally, bar lines) from a page image so
 * that what was written on the staff (tab numbers, noteheads) stands
 * alone. Only long runs along a staff line are removed, and only where
 * nothing continues above and below the line, so a number or notehead
 * crossing the line keeps its pixels.
 *
 * @property barLineCoverage Share of the staff height a vertical run must span to be a bar line. Type: Double
 */
class StaffLineEraser(private val barLineCoverage: Double = DEFAULT_BAR_LINE_COVERAGE) {

    /**
     * @param image Page image. Type: BinaryImage
     * @param staffs Staffs found on it. Type: List<Staff>
     * @param eraseBarLines True to erase bar lines too. Type: Boolean
     * @return A copy without the staff lines (and bar lines). Type: BinaryImage
     */
    fun erase(image: BinaryImage, staffs: List<Staff>, eraseBarLines: Boolean = true): BinaryImage {
        val erased = image.copy()
        staffs.forEach { staff ->
            if (eraseBarLines) eraseBarLines(image, erased, staff)
            eraseStaffLines(image, erased, staff)
        }
        return erased
    }

    /**
     * @param source Original image, where bar lines are still whole. Type: BinaryImage
     * @param target Image to erase from. Type: BinaryImage
     * @param staff Staff whose bar lines are erased. Type: Staff
     * @return Unit
     */
    private fun eraseBarLines(source: BinaryImage, target: BinaryImage, staff: Staff) {
        val topRow = floor(staff.top - staff.lineThickness).toInt()
        val bottomRow = ceil(staff.bottom + staff.lineThickness).toInt()
        val neededRows = (staff.bottom - staff.top) * barLineCoverage
        for (x in staff.left..staff.right) {
            var longestRun = 0
            var currentRun = 0
            for (y in topRow..bottomRow) {
                currentRun = if (source.isInk(x, y)) currentRun + 1 else 0
                longestRun = maxOf(longestRun, currentRun)
            }
            if (longestRun >= neededRows) {
                for (y in topRow..bottomRow) target.setInk(x, y, false)
            }
        }
    }

    /**
     * @param source Original image, used to see what continues above and below a line. Type: BinaryImage
     * @param target Image to erase from. Type: BinaryImage
     * @param staff Staff whose lines are erased. Type: Staff
     * @return Unit
     */
    private fun eraseStaffLines(source: BinaryImage, target: BinaryImage, staff: Staff) {
        val longestKeptRun = staff.lineSpacing.toInt()
        staff.lineYs.forEach { lineY ->
            val firstRow = floor(lineY - staff.lineThickness / 2 - 1).toInt()
            val lastRow = ceil(lineY + staff.lineThickness / 2).toInt()
            /** True where a symbol crosses the line: ink just above and just below it. */
            fun isCrossedAt(x: Int): Boolean = source.isInk(x, firstRow - 1) && source.isInk(x, lastRow + 1)
            for (y in firstRow..lastRow) {
                var runStart = -1
                for (x in 0..target.width) {
                    val isInk = x < target.width && target.isInk(x, y)
                    if (isInk && runStart < 0) runStart = x
                    if (!isInk && runStart >= 0) {
                        if (x - runStart > longestKeptRun) {
                            for (runX in runStart until x) if (!isCrossedAt(runX)) target.setInk(runX, y, false)
                        }
                        runStart = -1
                    }
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_BAR_LINE_COVERAGE = 0.9
    }
}
