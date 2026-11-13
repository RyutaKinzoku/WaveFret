package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.BinaryImage

/** Draws simple engraved-music shapes into a [BinaryImage] for tests. */
object ScoreImageDrawing {

    /** Draws a horizontal line [thickness] pixels thick from [left] to [right] at row [y]. */
    fun horizontalLine(image: BinaryImage, y: Int, left: Int, right: Int, thickness: Int = 2) {
        for (row in y until y + thickness) for (x in left..right) image.setInk(x, row, true)
    }

    /** Draws a vertical line from [top] to [bottom] at column [x]. */
    fun verticalLine(image: BinaryImage, x: Int, top: Int, bottom: Int, thickness: Int = 2) {
        for (column in x until x + thickness) for (y in top..bottom) image.setInk(column, y, true)
    }

    /** Draws a staff of [lineCount] lines [spacing] pixels apart, top line at [top]. Returns each line's row. */
    fun staff(image: BinaryImage, top: Int, lineCount: Int, spacing: Int, left: Int, right: Int): List<Int> {
        return List(lineCount) { index -> (top + index * spacing).also { horizontalLine(image, it, left, right) } }
    }

    /** Clears a box, as engravers do behind tab numbers. */
    fun clearBox(image: BinaryImage, left: Int, top: Int, width: Int, height: Int) {
        for (y in top until top + height) for (x in left until left + width) image.setInk(x, y, false)
    }

    /** Draws a filled ellipse, like a filled notehead, centred at ([centreX], [centreY]). */
    fun filledNotehead(image: BinaryImage, centreX: Int, centreY: Int, halfWidth: Int, halfHeight: Int) {
        for (y in centreY - halfHeight..centreY + halfHeight) for (x in centreX - halfWidth..centreX + halfWidth) {
            val dx = (x - centreX).toDouble() / halfWidth
            val dy = (y - centreY).toDouble() / halfHeight
            if (dx * dx + dy * dy <= 1.0) image.setInk(x, y, true)
        }
    }

    /** Draws a hollow ellipse, like a half or whole notehead. */
    fun hollowNotehead(image: BinaryImage, centreX: Int, centreY: Int, halfWidth: Int, halfHeight: Int) {
        for (y in centreY - halfHeight..centreY + halfHeight) for (x in centreX - halfWidth..centreX + halfWidth) {
            val dx = (x - centreX).toDouble() / halfWidth
            val dy = (y - centreY).toDouble() / halfHeight
            val distance = dx * dx + dy * dy
            if (distance <= 1.0 && distance >= 0.45) image.setInk(x, y, true)
        }
    }

    /** Draws a filled notehead with a stem rising from its right edge, [spacing] being the staff spacing. */
    fun quarterNote(image: BinaryImage, centreX: Int, centreY: Int, spacing: Int) {
        filledNotehead(image, centreX, centreY, halfWidth = spacing * 7 / 12, halfHeight = spacing * 5 / 12)
        verticalLine(image, centreX + spacing * 7 / 12 - 1, centreY - spacing * 7 / 2, centreY)
    }

    /** Draws a hollow notehead with a stem (a half note). */
    fun halfNote(image: BinaryImage, centreX: Int, centreY: Int, spacing: Int) {
        hollowNotehead(image, centreX, centreY, halfWidth = spacing * 7 / 12, halfHeight = spacing * 5 / 12)
        verticalLine(image, centreX + spacing * 7 / 12 - 1, centreY - spacing * 7 / 2, centreY)
    }

    /** Draws a hollow notehead without a stem (a whole note). */
    fun wholeNote(image: BinaryImage, centreX: Int, centreY: Int, spacing: Int) {
        hollowNotehead(image, centreX, centreY, halfWidth = spacing * 7 / 12, halfHeight = spacing * 5 / 12)
    }

    /** Draws a flat sign whose bowl sits at row [positionY]. */
    fun flat(image: BinaryImage, x: Int, positionY: Int, spacing: Int) {
        verticalLine(image, x, positionY - spacing * 9 / 4, positionY + spacing / 2)
        hollowNotehead(image, x + spacing / 3, positionY + spacing / 6, halfWidth = spacing / 3, halfHeight = spacing / 3)
    }

    /** Draws a sharp sign centred on row [positionY]. */
    fun sharp(image: BinaryImage, centreX: Int, positionY: Int, spacing: Int) {
        val halfHeight = spacing * 3 / 2
        verticalLine(image, centreX - spacing / 4, positionY - halfHeight, positionY + halfHeight)
        verticalLine(image, centreX + spacing / 4, positionY - halfHeight, positionY + halfHeight)
        horizontalLine(image, positionY - spacing / 3, centreX - spacing / 2, centreX + spacing / 2)
        horizontalLine(image, positionY + spacing / 3, centreX - spacing / 2, centreX + spacing / 2)
    }
}
