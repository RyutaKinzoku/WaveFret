package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.BinaryImage

/**
 * Tells a treble clef from a bass clef at the start of a music staff by
 * its height: a treble clef reaches well above and below the staff,
 * while a bass clef stays inside it.
 *
 * @property staffLineEraser Removes the staff lines, keeping symbols that cross them. Type: StaffLineEraser
 * @property staffHeaderFinder Finds the clef. Type: StaffHeaderFinder
 */
class ClefDetector(
    private val staffLineEraser: StaffLineEraser = StaffLineEraser(),
    private val staffHeaderFinder: StaffHeaderFinder = StaffHeaderFinder()
) {

    /**
     * @param image Page image. Type: BinaryImage
     * @param staff A 5-line staff on it. Type: Staff
     * @return The staff's clef, or [Clef.UNKNOWN] if none is found. Type: Clef
     */
    fun detect(image: BinaryImage, staff: Staff): Clef {
        val withoutLines = staffLineEraser.erase(image, listOf(staff), eraseBarLines = false)
        val clefShape = staffHeaderFinder.find(withoutLines, staff)?.clef ?: return Clef.UNKNOWN
        val overhang = OVERHANG_SPACINGS * staff.lineSpacing
        val reachesOutside = clefShape.top < staff.top - overhang || clefShape.bottom > staff.bottom + overhang
        return if (reachesOutside) Clef.TREBLE else Clef.BASS
    }

    private companion object {
        const val OVERHANG_SPACINGS = 0.75
    }
}
