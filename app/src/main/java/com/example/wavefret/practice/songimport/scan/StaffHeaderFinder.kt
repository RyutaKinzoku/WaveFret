package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.BinaryImage
import com.example.wavefret.common.imaging.PixelRegion
import com.example.wavefret.common.imaging.connectedInkShapes
import kotlin.math.roundToInt

/**
 * Finds the header of a music staff: the clef, which starts a little
 * after the staff's first column (so the line joining a piano system is
 * not taken for it), and the small symbols packed right after it (key
 * signature, time signature). The header ends at the first gap or at the
 * first shape too wide or too far from the staff to be one of them,
 * which is where the notes begin.
 */
class StaffHeaderFinder {

    /**
     * @param withoutLines Page image with the staff lines erased. Type: BinaryImage
     * @param staff A 5-line staff on it. Type: Staff
     * @return The staff's header, or null if no clef is found. Type: StaffHeader?
     */
    fun find(withoutLines: BinaryImage, staff: Staff): StaffHeader? {
        val spacing = staff.lineSpacing
        val searchRegion = PixelRegion(
            left = staff.left,
            top = (staff.top - SEARCH_MARGIN_SPACINGS * spacing).roundToInt(),
            right = (staff.left + SEARCH_WIDTH_SPACINGS * spacing).roundToInt(),
            bottom = (staff.bottom + SEARCH_MARGIN_SPACINGS * spacing).roundToInt()
        ).clampedTo(withoutLines)
        val shapes = withoutLines.connectedInkShapes(searchRegion).sortedBy { it.left }
        val clef = shapes
            .filter { shape ->
                shape.left >= staff.left + CLEF_EARLIEST_START_SPACINGS * spacing &&
                    shape.left <= staff.left + CLEF_LATEST_START_SPACINGS * spacing &&
                    shape.width >= CLEF_MINIMUM_WIDTH_SPACINGS * spacing &&
                    shape.height >= CLEF_MINIMUM_HEIGHT_SPACINGS * spacing
            }
            .maxByOrNull { it.height } ?: return null

        val nearTop = staff.top - SYMBOL_OVERHANG_SPACINGS * spacing
        val nearBottom = staff.bottom + SYMBOL_OVERHANG_SPACINGS * spacing
        val symbols = mutableListOf<PixelRegion>()
        var endColumn = clef.right
        for (shape in shapes) {
            if (shape == clef || shape.left < clef.left) continue
            if (shape.bottom < nearTop || shape.top > nearBottom) continue
            if (shape.left > endColumn + MAXIMUM_GAP_SPACINGS * spacing) break
            val fitsAHeader = shape.width <= MAXIMUM_SYMBOL_WIDTH_SPACINGS * spacing && shape.top >= nearTop && shape.bottom <= nearBottom
            if (!fitsAHeader) break
            symbols += shape
            endColumn = maxOf(endColumn, shape.right)
        }
        return StaffHeader(clef, symbols, endColumn)
    }

    private companion object {
        const val SEARCH_MARGIN_SPACINGS = 3.0
        const val SEARCH_WIDTH_SPACINGS = 14.0
        const val CLEF_EARLIEST_START_SPACINGS = 0.4
        const val CLEF_LATEST_START_SPACINGS = 2.5
        const val CLEF_MINIMUM_WIDTH_SPACINGS = 1.5
        const val CLEF_MINIMUM_HEIGHT_SPACINGS = 2.0
        const val SYMBOL_OVERHANG_SPACINGS = 1.6
        const val MAXIMUM_GAP_SPACINGS = 1.0
        const val MAXIMUM_SYMBOL_WIDTH_SPACINGS = 1.6
    }
}
