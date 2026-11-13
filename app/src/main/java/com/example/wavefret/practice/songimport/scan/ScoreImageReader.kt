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
 * lines, and, only when a page has no tab, reads music staffs note by
 * note. A staff is tab only when numbers sit on its lines: a 4-line staff
 * without them (a staff that lost a line in a poor scan) is skipped, and
 * a 5-line staff without them is a music staff. When a page has 4-line
 * tab, its 5-line staffs are not read at all. On pages with both treble-clef and
 * bass-clef staffs (piano and vocal scores) only the bass-clef staffs are
 * read, since they hold the bass line.
 *
 * @property tuning Tuning tabs are read in. Type: BassTuning
 * @property symbolRecognizer Reads the numbers around each tab staff. Type: ImageSymbolRecognizer
 * @property staffFinder Finds staffs. Type: StaffFinder
 * @property staffLineEraser Removes lines before numbers are read. Type: StaffLineEraser
 * @property notationStaffReader Reads music staffs. Type: NotationStaffReader
 * @property clefDetector Tells treble from bass clef. Type: ClefDetector
 */
class ScoreImageReader(
    tuning: BassTuning,
    private val symbolRecognizer: ImageSymbolRecognizer,
    private val staffFinder: StaffFinder = StaffFinder(),
    private val staffLineEraser: StaffLineEraser = StaffLineEraser(),
    private val notationStaffReader: NotationStaffReader = NotationStaffReader(staffLineEraser),
    private val clefDetector: ClefDetector = ClefDetector(staffLineEraser)
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

        val fourLineTab = staffs.filter { it.lineYs.size == TAB_LINE_COUNT }
            .mapNotNull { staff -> readTabIfNumbered(pageWithoutLines, staff, MINIMUM_NUMBERS_FOR_FOUR_LINE_TAB) }
            .filter { it.isNotEmpty() }
        if (fourLineTab.isNotEmpty()) return PageReading(fourLineTab, emptyList())

        val fiveLineTab = mutableListOf<List<List<TabNote>>>()
        val musicStaffs = mutableListOf<Staff>()
        staffs.filter { it.lineYs.size == MUSIC_LINE_COUNT }.forEach { staff ->
            val tab = readTabIfNumbered(pageWithoutLines, staff, MINIMUM_NUMBERS_FOR_FIVE_LINE_TAB)
            if (tab == null) musicStaffs += staff else if (tab.isNotEmpty()) fiveLineTab += tab
        }
        if (fiveLineTab.isNotEmpty()) return PageReading(fiveLineTab, emptyList())
        val clefs = musicStaffs.associateWith { clefDetector.detect(page, it) }
        val hasBassClef = clefs.values.any { it == Clef.BASS }
        val notationSystems = musicStaffs
            .filter { !hasBassClef || clefs.getValue(it) == Clef.BASS }
            .map { notationStaffReader.read(page, it, clefs.getValue(it)) }
            .filter { it.isNotEmpty() }
        return PageReading(emptyList(), notationSystems)
    }

    /**
     * @param pageWithoutLines Page with staff and bar lines erased. Type: BinaryImage
     * @param staff A 4- or 5-line staff. Type: Staff
     * @param numbersNeeded Numbers that must sit on its lines for it to be tab. Type: Int
     * @return Its notes as columns if it is tab, or null if it is not. Type: List<List<TabNote>>?
     */
    private suspend fun readTabIfNumbered(pageWithoutLines: BinaryImage, staff: Staff, numbersNeeded: Int): List<List<TabNote>>? {
        val symbols = symbolRecognizer.recognize(pageWithoutLines, regionAround(staff, pageWithoutLines))
        return if (drawnTabReader.countNumbersOnLines(staff, symbols) >= numbersNeeded) drawnTabReader.read(staff, symbols) else null
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
        const val MINIMUM_NUMBERS_FOR_FOUR_LINE_TAB = 1
        const val MINIMUM_NUMBERS_FOR_FIVE_LINE_TAB = 3
        const val REGION_MARGIN_SPACINGS = 1.5
    }
}
