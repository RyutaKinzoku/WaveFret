package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.BinaryImage
import com.example.wavefret.common.imaging.PixelRegion
import com.example.wavefret.common.imaging.connectedInkShapes
import kotlin.math.roundToInt

/**
 * Tells a treble clef from a bass clef at the start of a music staff by
 * its height: a treble clef reaches well above and below the staff,
 * while a bass clef stays inside it. Thin vertical lines (the line
 * joining a piano system, bar lines) are not clefs.
 *
 * @property staffLineEraser Removes the staff lines, keeping symbols that cross them. Type: StaffLineEraser
 */
class ClefDetector(private val staffLineEraser: StaffLineEraser = StaffLineEraser()) {

    /**
     * @param image Page image. Type: BinaryImage
     * @param staff A 5-line staff on it. Type: Staff
     * @return The staff's clef, or [Clef.UNKNOWN] if none is found. Type: Clef
     */
    fun detect(image: BinaryImage, staff: Staff): Clef {
        val spacing = staff.lineSpacing
        val searchRegion = PixelRegion(
            left = staff.left,
            top = (staff.top - SEARCH_MARGIN_SPACINGS * spacing).roundToInt(),
            right = (staff.left + SEARCH_WIDTH_SPACINGS * spacing).roundToInt(),
            bottom = (staff.bottom + SEARCH_MARGIN_SPACINGS * spacing).roundToInt()
        ).clampedTo(image)
        val withoutLines = staffLineEraser.erase(image, listOf(staff), eraseBarLines = false)
        val clefShape = withoutLines.connectedInkShapes(searchRegion)
            .filter { shape ->
                shape.left <= staff.left + CLEF_START_SPACINGS * spacing &&
                    shape.width >= MINIMUM_WIDTH_SPACINGS * spacing &&
                    shape.height >= MINIMUM_HEIGHT_SPACINGS * spacing
            }
            .maxByOrNull { it.height } ?: return Clef.UNKNOWN
        val overhang = OVERHANG_SPACINGS * spacing
        val reachesOutside = clefShape.top < staff.top - overhang || clefShape.bottom > staff.bottom + overhang
        return if (reachesOutside) Clef.TREBLE else Clef.BASS
    }

    private companion object {
        const val SEARCH_MARGIN_SPACINGS = 3.0
        const val SEARCH_WIDTH_SPACINGS = 4.5
        const val CLEF_START_SPACINGS = 2.0
        const val MINIMUM_WIDTH_SPACINGS = 0.5
        const val MINIMUM_HEIGHT_SPACINGS = 2.0
        const val OVERHANG_SPACINGS = 0.75
    }
}
