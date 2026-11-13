package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.BinaryImage
import com.example.wavefret.common.imaging.PixelRegion
import com.example.wavefret.common.music.BassTuning
import com.example.wavefret.common.textrecognition.ImageSymbolRecognizer
import com.example.wavefret.practice.TabNote
import kotlin.math.roundToInt

/**
 * Reads the music on a page image (a PDF page, a photo or a video frame):
 * finds the staffs, reads tab staffs by recognizing the numbers on their
 * lines, and, only when a page has no tab, reads bass-clef music staffs
 * note by note. A 5-line staff with numbers on its lines is a 5-string
 * bass tab; without them it is a music staff.
 *
 * @property tuning Tuning tabs are read in. Type: BassTuning
 * @property symbolRecognizer Reads the numbers around each tab staff. Type: ImageSymbolRecognizer
 * @property staffFinder Finds staffs. Type: StaffFinder
 * @property staffLineEraser Removes lines before numbers are read. Type: StaffLineEraser
 * @property notationStaffReader Reads music staffs. Type: NotationStaffReader
 */
class ScoreImageReader(
    tuning: BassTuning,
    private val symbolRecognizer: ImageSymbolRecognizer,
    private val staffFinder: StaffFinder = StaffFinder(),
    private val staffLineEraser: StaffLineEraser = StaffLineEraser(),
    private val notationStaffReader: NotationStaffReader = NotationStaffReader(staffLineEraser)
) {

    private val drawnTabReader = DrawnTabReader(tuning)

    /**
     * @param page Page image. Type: BinaryImage
     * @return The page's tab systems, or its music systems when it has no tab. Type: PageReading
     */
    suspend fun read(page: BinaryImage): PageReading {
        val staffs = staffFinder.find(page)
        if (staffs.isEmpty()) return PageReading(emptyList(), emptyList())
        val pageWithoutLines = staffLineEraser.erase(page, staffs)
        val hasFourLineStaff = staffs.any { it.lineYs.size == TAB_LINE_COUNT }

        val tabSystems = mutableListOf<List<List<TabNote>>>()
        val musicStaffs = mutableListOf<Staff>()
        for (staff in staffs) {
            when {
                staff.lineYs.size == TAB_LINE_COUNT -> tabSystems += readTab(pageWithoutLines, staff)
                staff.lineYs.size == MUSIC_LINE_COUNT && hasFourLineStaff -> Unit
                staff.lineYs.size == MUSIC_LINE_COUNT -> {
                    val symbols = symbolRecognizer.recognize(pageWithoutLines, regionAround(staff, page))
                    if (drawnTabReader.countNumbersOnLines(staff, symbols) >= MINIMUM_NUMBERS_FOR_TAB) {
                        tabSystems += drawnTabReader.read(staff, symbols)
                    } else {
                        musicStaffs += staff
                    }
                }
            }
        }
        if (tabSystems.isNotEmpty()) return PageReading(tabSystems.filter { it.isNotEmpty() }, emptyList())
        val notationSystems = musicStaffs.map { notationStaffReader.read(page, it) }.filter { it.isNotEmpty() }
        return PageReading(emptyList(), notationSystems)
    }

    /**
     * @param pageWithoutLines Page with staff and bar lines erased. Type: BinaryImage
     * @param staff A tab staff. Type: Staff
     * @return Its notes as columns. Type: List<List<TabNote>>
     */
    private suspend fun readTab(pageWithoutLines: BinaryImage, staff: Staff): List<List<TabNote>> {
        val symbols = symbolRecognizer.recognize(pageWithoutLines, regionAround(staff, pageWithoutLines))
        return drawnTabReader.read(staff, symbols)
    }

    /**
     * @param staff A staff. Type: Staff
     * @param page Its page. Type: BinaryImage
     * @return The staff with a margin, where its numbers are printed. Type: PixelRegion
     */
    private fun regionAround(staff: Staff, page: BinaryImage): PixelRegion {
        val margin = staff.lineSpacing * REGION_MARGIN_SPACINGS
        return PixelRegion(
            left = (staff.left - staff.lineSpacing).roundToInt(),
            top = (staff.top - margin).roundToInt(),
            right = (staff.right + staff.lineSpacing).roundToInt(),
            bottom = (staff.bottom + margin).roundToInt()
        ).clampedTo(page)
    }

    private companion object {
        const val TAB_LINE_COUNT = 4
        const val MUSIC_LINE_COUNT = 5
        const val MINIMUM_NUMBERS_FOR_TAB = 3
        const val REGION_MARGIN_SPACINGS = 1.5
    }
}
