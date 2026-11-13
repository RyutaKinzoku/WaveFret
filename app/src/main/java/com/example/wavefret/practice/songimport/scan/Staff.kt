package com.example.wavefret.practice.songimport.scan

/**
 * A group of evenly spaced horizontal lines found on a page: a 5-line
 * music staff or a 4-line bass tab staff.
 *
 * @property lineYs Vertical centre of each line, top line first. Type: List<Double>
 * @property lineThickness Typical line thickness in pixels. Type: Double
 * @property left Leftmost column of the lines. Type: Int
 * @property right Rightmost column of the lines. Type: Int
 */
data class Staff(
    val lineYs: List<Double>,
    val lineThickness: Double,
    val left: Int,
    val right: Int
) {
    /** Distance between neighbouring lines in pixels. Type: Double */
    val lineSpacing: Double get() = (lineYs.last() - lineYs.first()) / (lineYs.size - 1)

    /** Centre of the top line. Type: Double */
    val top: Double get() = lineYs.first()

    /** Centre of the bottom line. Type: Double */
    val bottom: Double get() = lineYs.last()
}
