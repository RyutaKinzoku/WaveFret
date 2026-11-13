package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.BinaryImage
import com.example.wavefret.common.imaging.PixelRegion
import com.example.wavefret.common.imaging.connectedInkShapes
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Reads the notes of one music staff from a page image (experimental
 * optical music recognition). It finds filled noteheads with a stem
 * (quarter notes and shorter) and hollow noteheads (half and whole notes),
 * turns their height on the staff into a pitch for the staff's clef,
 * applies the key signature and sharps or flats written just before a
 * note, and returns the pitches to play on the bass, left to right (see
 * [Clef.semitonesToBass]). The clef, key signature and time signature
 * at the start of the staff are skipped, and the key signature is only
 * trusted in the standard order of sharps (F C G D A E B) or flats
 * (B E A D G C F). Rhythm is not read, and of notes played together only
 * the lowest is kept.
 *
 * @property staffLineEraser Removes the staff lines before shapes are measured. Type: StaffLineEraser
 * @property staffHeaderFinder Finds the clef, key signature and time signature. Type: StaffHeaderFinder
 */
class NotationStaffReader(
    private val staffLineEraser: StaffLineEraser = StaffLineEraser(),
    private val staffHeaderFinder: StaffHeaderFinder = StaffHeaderFinder()
) {

    /**
     * @param image Page image. Type: BinaryImage
     * @param staff A 5-line music staff on it. Type: Staff
     * @param clef The staff's clef. Type: Clef
     * @return MIDI numbers to play on the bass for the staff's notes, left to right. Type: List<Int>
     */
    fun read(image: BinaryImage, staff: Staff, clef: Clef = Clef.BASS): List<Int> {
        val spacing = staff.lineSpacing
        val bandTop = (staff.top - BAND_SPACINGS * spacing).toInt().coerceAtLeast(0)
        val bandBottom = (staff.bottom + BAND_SPACINGS * spacing).toInt().coerceAtMost(image.height - 1)
        val working = staffLineEraser.erase(image, listOf(staff), eraseBarLines = false)
        eraseLedgerLines(working, staff, bandTop, bandBottom)
        val shapes = ShapeMeasurements(working, staff.left, staff.right, bandTop, bandBottom)

        val header = staffHeaderFinder.find(working, staff)
        val firstNoteColumn = header?.endColumn?.toDouble() ?: (staff.left + CLEF_WIDTH_SPACINGS * spacing)
        val noteheads = (findFilledNoteheads(shapes, staff) + findHollowNoteheads(shapes, staff))
            .filter { it.centreX > firstNoteColumn }
            .let(::withoutDuplicates)
            .sortedBy { it.centreX }
        if (noteheads.isEmpty()) return emptyList()

        val firstAccidentalColumn = header?.endColumn?.toDouble() ?: (staff.left + KEY_SIGNATURE_START_SPACINGS * spacing)
        val accidentals = findAccidentals(working, staff, bandTop..bandBottom, firstAccidentalColumn, noteheads)
        val inlineAccidentals = noteheads.associateWith { notehead ->
            accidentals.firstOrNull { accidental ->
                accidental.right <= notehead.left + spacing * INLINE_OVERLAP_SPACINGS &&
                    notehead.left - accidental.right <= spacing * INLINE_REACH_SPACINGS &&
                    abs(accidental.position - positionOf(notehead.centreY, staff)) <= 1
            }
        }
        val usedInline = inlineAccidentals.values.filterNotNull().toSet()
        val keySignature = if (header != null) {
            keySignatureOf(header, working, staff, clef)
        } else {
            accidentals
                .filter { it !in usedInline && it.right < noteheads.first().left }
                .associate { letterIndexOf(it.position, clef) to if (it.isFlat) -1 else 1 }
        }

        val columns = mutableListOf<MutableList<Notehead>>()
        noteheads.forEach { notehead ->
            val column = columns.lastOrNull()
            if (column != null && notehead.centreX - column.first().centreX <= spacing * CHORD_WIDTH_SPACINGS) column += notehead
            else columns += mutableListOf(notehead)
        }
        return columns.map { column ->
            column.minOf { notehead ->
                val position = positionOf(notehead.centreY, staff)
                val letterIndex = letterIndexOf(position, clef)
                val keyAlteration = keySignature[letterIndex] ?: 0
                val alteration = when (val accidental = inlineAccidentals[notehead]) {
                    null -> keyAlteration
                    else -> if (accidental.isFlat) -1 else if (keyAlteration < 0) 0 else 1
                }
                writtenNaturalMidiNumber(position, clef) + alteration + clef.semitonesToBass
            }
        }
    }

    /**
     * Ledger lines are thin horizontal runs above and below the staff; they
     * would merge with noteheads, so they are erased where nothing crosses them.
     *
     * @param working Image to erase from. Type: BinaryImage
     * @param staff The staff. Type: Staff
     * @param bandTop First row searched. Type: Int
     * @param bandBottom Last row searched. Type: Int
     * @return Unit
     */
    private fun eraseLedgerLines(working: BinaryImage, staff: Staff, bandTop: Int, bandBottom: Int) {
        val thinLimit = (staff.lineThickness + 1).roundToInt()
        val minimumLength = (staff.lineSpacing * LEDGER_MIN_LENGTH_SPACINGS).roundToInt()
        val toErase = mutableListOf<Pair<Int, Int>>()
        for (y in bandTop..bandBottom) {
            if (y >= staff.top - staff.lineSpacing / 2 && y <= staff.bottom + staff.lineSpacing / 2) continue
            var runStart = -1
            for (x in staff.left..staff.right + 1) {
                val isInk = x <= staff.right && working.isInk(x, y)
                if (isInk && runStart < 0) runStart = x
                if (!isInk && runStart >= 0) {
                    if (x - runStart >= minimumLength) {
                        for (runX in runStart until x) if (verticalRunAt(working, runX, y) <= thinLimit) toErase += runX to y
                    }
                    runStart = -1
                }
            }
        }
        toErase.forEach { (x, y) -> working.setInk(x, y, false) }
    }

    /**
     * @param shapes Ink measurements of the staff band. Type: ShapeMeasurements
     * @param staff The staff. Type: Staff
     * @return Solid notehead-sized shapes with a stem beside them. Type: List<Notehead>
     */
    private fun findFilledNoteheads(shapes: ShapeMeasurements, staff: Staff): List<Notehead> {
        val spacing = staff.lineSpacing
        val boxHalfWidth = (spacing * FILLED_BOX_HALF_WIDTH_SPACINGS).roundToInt()
        val boxHalfHeight = (spacing * FILLED_BOX_HALF_HEIGHT_SPACINGS).roundToInt()
        val boxArea = (2 * boxHalfWidth + 1) * (2 * boxHalfHeight + 1)
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (y in shapes.top + boxHalfHeight..shapes.bottom - boxHalfHeight) {
            for (x in shapes.left + boxHalfWidth..shapes.right - boxHalfWidth) {
                val inkCount = shapes.inkCount(x - boxHalfWidth, y - boxHalfHeight, x + boxHalfWidth, y + boxHalfHeight)
                if (inkCount >= boxArea * FILLED_DENSITY) candidates += x to y
            }
        }
        return clusters(candidates, spacing)
            .filter { notehead -> isNoteheadWide(shapes.image, notehead, spacing) && hasStem(shapes, notehead, spacing) }
    }

    /**
     * Beams are as solid as noteheads but much wider.
     *
     * @param image Image without staff lines. Type: BinaryImage
     * @param notehead A solid shape. Type: Notehead
     * @param spacing Staff line spacing. Type: Double
     * @return True if the ink through its centre is no wider than a notehead. Type: Boolean
     */
    private fun isNoteheadWide(image: BinaryImage, notehead: Notehead, spacing: Double): Boolean {
        val y = notehead.centreY.toInt()
        var left = notehead.centreX.toInt()
        while (image.isInk(left - 1, y)) left--
        var right = notehead.centreX.toInt()
        while (image.isInk(right + 1, y)) right++
        return right - left + 1 <= spacing * MAXIMUM_HEAD_WIDTH_SPACINGS
    }

    /**
     * @param shapes Ink measurements of the staff band. Type: ShapeMeasurements
     * @param staff The staff. Type: Staff
     * @return Notehead-sized rings with an empty middle. Type: List<Notehead>
     */
    private fun findHollowNoteheads(shapes: ShapeMeasurements, staff: Staff): List<Notehead> {
        val spacing = staff.lineSpacing
        val radiusX = spacing * HOLLOW_RING_RADIUS_X_SPACINGS
        val radiusY = spacing * HOLLOW_RING_RADIUS_Y_SPACINGS
        val ringPoints = List(RING_SAMPLE_COUNT) { index ->
            val angle = 2 * Math.PI * index / RING_SAMPLE_COUNT
            (radiusX * cos(angle)).roundToInt() to (radiusY * sin(angle)).roundToInt()
        }
        val holeReach = (spacing * HOLLOW_HOLE_REACH_SPACINGS).roundToInt()
        val holePoints = listOf(0 to 0, holeReach to 0, -holeReach to 0)
        val margin = radiusX.roundToInt() + 1
        val candidates = mutableListOf<Pair<Int, Int>>()
        for (y in shapes.top + margin..shapes.bottom - margin) {
            for (x in shapes.left + margin..shapes.right - margin) {
                if (holePoints.any { (dx, dy) -> shapes.image.isInk(x + dx, y + dy) }) continue
                val ringInk = ringPoints.count { (dx, dy) -> shapes.image.isInk(x + dx, y + dy) }
                if (ringInk >= RING_SAMPLE_COUNT * HOLLOW_RING_INK_SHARE) candidates += x to y
            }
        }
        return clusters(candidates, spacing)
    }

    /**
     * @param shapes Ink measurements. Type: ShapeMeasurements
     * @param notehead A solid shape. Type: Notehead
     * @param spacing Staff line spacing. Type: Double
     * @return True if a long vertical stroke starts at its left or right side and runs one way, up or down. Type: Boolean
     */
    private fun hasStem(shapes: ShapeMeasurements, notehead: Notehead, spacing: Double): Boolean {
        val sideOffset = (spacing * STEM_SIDE_OFFSET_SPACINGS).roundToInt()
        val reach = (spacing * STEM_SEARCH_REACH_SPACINGS).roundToInt()
        val minimumStem = spacing * MINIMUM_STEM_SPACINGS
        val centreX = notehead.centreX.roundToInt()
        val centreY = notehead.centreY.roundToInt()
        val columns = (centreX - sideOffset - reach..centreX - sideOffset + reach) + (centreX + sideOffset - reach..centreX + sideOffset + reach)
        val nearEnd = spacing * STEM_NEAR_END_SPACINGS
        return columns.any { x ->
            (centreY - reach..centreY + reach).any { y ->
                val (runTop, runBottom) = verticalRunEnds(shapes.image, x, y) ?: return@any false
                val rises = centreY - runTop >= minimumStem && runBottom - centreY <= nearEnd
                val falls = runBottom - centreY >= minimumStem && centreY - runTop <= nearEnd
                rises || falls
            }
        }
    }

    /**
     * @param working Image without staff lines. Type: BinaryImage
     * @param staff The staff. Type: Staff
     * @param band Rows searched. Type: IntRange
     * @param firstAccidentalColumn Leftmost column an accidental may start at. Type: Double
     * @param noteheads Noteheads found, whose shapes are not accidentals. Type: List<Notehead>
     * @return Sharps, naturals and flats found on the staff. Type: List<Accidental>
     */
    private fun findAccidentals(
        working: BinaryImage,
        staff: Staff,
        band: IntRange,
        firstAccidentalColumn: Double,
        noteheads: List<Notehead>
    ): List<Accidental> {
        val spacing = staff.lineSpacing
        return working.connectedInkShapes(PixelRegion(staff.left, band.first, staff.right, band.last))
            .filter { shape ->
                val shapeHeight = shape.bottom - shape.top + 1
                val shapeWidth = shape.right - shape.left + 1
                shape.left >= firstAccidentalColumn &&
                    shapeHeight >= spacing * ACCIDENTAL_MIN_HEIGHT_SPACINGS && shapeHeight <= spacing * ACCIDENTAL_MAX_HEIGHT_SPACINGS &&
                    shapeWidth <= spacing * ACCIDENTAL_MAX_WIDTH_SPACINGS &&
                    noteheads.none { it.centreX.roundToInt() in shape.left..shape.right && it.centreY.roundToInt() in shape.top..shape.bottom }
            }
            .mapNotNull { shape -> accidentalOf(working, shape, staff) }
    }

    /**
     * A flat has one tall stroke with its bowl at the bottom, so most of
     * its ink is in its lower half; anything else with strokes (a sharp, a
     * natural, or a sharp blurred into a block by a poor scan) is read as
     * raising the note.
     *
     * @param working Image without staff lines. Type: BinaryImage
     * @param shape Bounding box of an accidental-sized shape. Type: PixelRegion
     * @param staff The staff. Type: Staff
     * @return The accidental, or null if the shape has no tall stroke. Type: Accidental?
     */
    private fun accidentalOf(working: BinaryImage, shape: PixelRegion, staff: Staff): Accidental? {
        val spacing = staff.lineSpacing
        val strokeCount = verticalStrokeCount(working, shape, spacing)
        if (strokeCount == 0) return null
        val middleRow = (shape.top + shape.bottom) / 2
        var upperInk = 0
        var lowerInk = 0
        for (y in shape.top..shape.bottom) for (x in shape.left..shape.right) {
            if (!working.isInk(x, y)) continue
            if (y <= middleRow) upperInk++ else lowerInk++
        }
        val isFlat = strokeCount == 1 && lowerInk > upperInk * FLAT_LOWER_HEAVY_RATIO
        val position = if (isFlat) {
            positionOf(shape.bottom + 1 - spacing * FLAT_BOWL_ABOVE_BOTTOM_SPACINGS, staff)
        } else {
            positionOf((shape.top + shape.bottom + 1) / 2.0, staff)
        }
        return Accidental(isFlat = isFlat, position = position, right = shape.right)
    }

    /**
     * @param header The staff's header. Type: StaffHeader
     * @param working Image without staff lines. Type: BinaryImage
     * @param staff The staff. Type: Staff
     * @param clef The staff's clef. Type: Clef
     * @return Alteration (+1 sharp, -1 flat) of each letter index in the key signature. Type: Map<Int, Int>
     */
    private fun keySignatureOf(header: StaffHeader, working: BinaryImage, staff: Staff, clef: Clef): Map<Int, Int> {
        val spacing = staff.lineSpacing
        val sizable = header.symbols.filter { it.height >= spacing * STACKED_SYMBOL_MIN_HEIGHT_SPACINGS }
        val accidentals = header.symbols
            .filter { symbol ->
                symbol.height >= spacing * KEY_ACCIDENTAL_MIN_HEIGHT_SPACINGS &&
                    symbol.height <= spacing * KEY_ACCIDENTAL_MAX_HEIGHT_SPACINGS &&
                    symbol.width <= spacing * ACCIDENTAL_MAX_WIDTH_SPACINGS &&
                    sizable.none { other -> other != symbol && isStacked(symbol, other) }
            }
            .mapNotNull { accidentalOf(working, it, staff) }
        val isFlatKey = accidentals.firstOrNull()?.isFlat ?: return emptyMap()
        val order = if (isFlatKey) FLAT_ORDER else SHARP_ORDER
        val letters = accidentals
            .takeWhile { it.isFlat == isFlatKey }
            .map { letterIndexOf(it.position, clef) }
            .zip(order.asList())
            .takeWhile { (letter, expected) -> letter == expected }
            .map { it.first }
        return letters.associateWith { if (isFlatKey) -1 else 1 }
    }

    /**
     * @param first A header symbol. Type: PixelRegion
     * @param second Another header symbol. Type: PixelRegion
     * @return True if one sits right above the other, like the two numbers of a time signature. Type: Boolean
     */
    private fun isStacked(first: PixelRegion, second: PixelRegion): Boolean {
        val overlap = minOf(first.right, second.right) - maxOf(first.left, second.left) + 1
        val isVerticallyApart = first.bottom < second.top || second.bottom < first.top
        return overlap * 2 >= minOf(first.width, second.width) && isVerticallyApart
    }

    /**
     * @param image Image without staff lines. Type: BinaryImage
     * @param shape Bounding box of a connected shape. Type: PixelRegion
     * @param spacing Staff line spacing. Type: Double
     * @return Number of separate tall vertical strokes in the shape (1 for a flat, 2 for a sharp or natural). Type: Int
     */
    private fun verticalStrokeCount(image: BinaryImage, shape: PixelRegion, spacing: Double): Int {
        val minimumStroke = spacing * ACCIDENTAL_STROKE_SPACINGS
        var strokeCount = 0
        var isInStroke = false
        for (x in shape.left..shape.right) {
            var longestRun = 0
            var currentRun = 0
            for (y in shape.top..shape.bottom) {
                currentRun = if (image.isInk(x, y)) currentRun + 1 else 0
                longestRun = maxOf(longestRun, currentRun)
            }
            val isStrokeColumn = longestRun >= minimumStroke
            if (isStrokeColumn && !isInStroke) strokeCount++
            isInStroke = isStrokeColumn
        }
        return strokeCount
    }

    /**
     * @param points Pixels that passed a notehead test. Type: List<Pair<Int, Int>>
     * @param spacing Staff line spacing. Type: Double
     * @return One notehead per group of neighbouring pixels, at their centre. Type: List<Notehead>
     */
    private fun clusters(points: List<Pair<Int, Int>>, spacing: Double): List<Notehead> {
        val reachX = spacing * CLUSTER_REACH_X_SPACINGS
        val reachY = spacing * CLUSTER_REACH_Y_SPACINGS
        val groups = mutableListOf<MutableList<Pair<Int, Int>>>()
        points.forEach { point ->
            val group = groups.firstOrNull { members ->
                members.any { abs(it.first - point.first) <= reachX && abs(it.second - point.second) <= reachY }
            }
            if (group != null) group += point else groups += mutableListOf(point)
        }
        return groups.map { members ->
            val centreX = members.map { it.first }.average() + CENTRE_OF_PIXEL
            val centreY = members.map { it.second }.average() + CENTRE_OF_PIXEL
            Notehead(centreX, centreY, left = members.minOf { it.first } - spacing * HEAD_HALF_WIDTH_SPACINGS)
        }
    }

    /**
     * @param noteheads Noteheads, possibly found twice. Type: List<Notehead>
     * @return The noteheads with near-identical ones merged. Type: List<Notehead>
     */
    private fun withoutDuplicates(noteheads: List<Notehead>): List<Notehead> {
        val kept = mutableListOf<Notehead>()
        noteheads.forEach { notehead ->
            if (kept.none { abs(it.centreX - notehead.centreX) < 2 * DUPLICATE_PIXELS && abs(it.centreY - notehead.centreY) < DUPLICATE_PIXELS }) {
                kept += notehead
            }
        }
        return kept
    }

    /**
     * @param image Image. Type: BinaryImage
     * @param x Column. Type: Int
     * @param y Row. Type: Int
     * @return Length of the vertical ink run through the pixel, 0 if it is paper. Type: Int
     */
    private fun verticalRunAt(image: BinaryImage, x: Int, y: Int): Int =
        verticalRunEnds(image, x, y)?.let { (top, bottom) -> bottom - top + 1 } ?: 0

    /**
     * @param image Image. Type: BinaryImage
     * @param x Column. Type: Int
     * @param y Row. Type: Int
     * @return Top and bottom rows of the vertical ink run through the pixel, or null if it is paper. Type: Pair<Int, Int>?
     */
    private fun verticalRunEnds(image: BinaryImage, x: Int, y: Int): Pair<Int, Int>? {
        if (!image.isInk(x, y)) return null
        var top = y
        while (image.isInk(x, top - 1)) top--
        var bottom = y
        while (image.isInk(x, bottom + 1)) bottom++
        return top to bottom
    }

    /**
     * @param centreY Vertical centre of a notehead or accidental. Type: Double
     * @param staff The staff. Type: Staff
     * @return Staff position: 0 is the bottom line, each step a line or space higher. Type: Int
     */
    private fun positionOf(centreY: Double, staff: Staff): Int = ((staff.bottom - centreY) / (staff.lineSpacing / 2)).roundToInt()

    /**
     * @param position Staff position. Type: Int
     * @param clef The staff's clef. Type: Clef
     * @return Letter index of the note there, from C (0) to B (6). Type: Int
     */
    private fun letterIndexOf(position: Int, clef: Clef): Int = Math.floorMod(clef.bottomLineStep + position, LETTERS_PER_OCTAVE)

    /**
     * @param position Staff position. Type: Int
     * @param clef The staff's clef. Type: Clef
     * @return Written MIDI number of the natural note there. Type: Int
     */
    private fun writtenNaturalMidiNumber(position: Int, clef: Clef): Int {
        val step = clef.bottomLineStep + position
        val octave = Math.floorDiv(step, LETTERS_PER_OCTAVE)
        return (octave + 1) * SEMITONES_PER_OCTAVE + SEMITONES_ABOVE_C[letterIndexOf(position, clef)]
    }

    /**
     * Fast ink counting over a region of the image.
     *
     * @property image Image measured. Type: BinaryImage
     * @property left Leftmost column. Type: Int
     * @property right Rightmost column. Type: Int
     * @property top Top row. Type: Int
     * @property bottom Bottom row. Type: Int
     */
    private class ShapeMeasurements(val image: BinaryImage, val left: Int, val right: Int, val top: Int, val bottom: Int) {
        private val regionWidth = right - left + 2
        private val summedInk = IntArray(regionWidth * (bottom - top + 2)).also { sums ->
            for (y in top..bottom) for (x in left..right) {
                val index = (y - top + 1) * regionWidth + (x - left + 1)
                sums[index] = (if (image.isInk(x, y)) 1 else 0) + sums[index - 1] + sums[index - regionWidth] - sums[index - regionWidth - 1]
            }
        }

        /**
         * @param fromX Left column, inclusive. Type: Int
         * @param fromY Top row, inclusive. Type: Int
         * @param toX Right column, inclusive. Type: Int
         * @param toY Bottom row, inclusive. Type: Int
         * @return Ink pixels in the box. Type: Int
         */
        fun inkCount(fromX: Int, fromY: Int, toX: Int, toY: Int): Int {
            fun sumAt(x: Int, y: Int): Int = summedInk[(y - top + 1) * regionWidth + (x - left + 1)]
            return sumAt(toX, toY) - sumAt(fromX - 1, toY) - sumAt(toX, fromY - 1) + sumAt(fromX - 1, fromY - 1)
        }
    }

    /**
     * @property centreX Horizontal centre in pixels. Type: Double
     * @property centreY Vertical centre in pixels. Type: Double
     * @property left Approximate left edge in pixels. Type: Double
     */
    private data class Notehead(val centreX: Double, val centreY: Double, val left: Double)

    /**
     * @property isFlat True for a flat, false for a sharp or natural. Type: Boolean
     * @property position Staff position it applies to. Type: Int
     * @property right Right edge in pixels. Type: Int
     */
    private data class Accidental(val isFlat: Boolean, val position: Int, val right: Int)

    private companion object {
        const val BAND_SPACINGS = 3.5
        const val CLEF_WIDTH_SPACINGS = 3.0
        const val KEY_SIGNATURE_START_SPACINGS = 2.5
        const val LEDGER_MIN_LENGTH_SPACINGS = 1.6
        const val FILLED_BOX_HALF_WIDTH_SPACINGS = 0.4
        const val FILLED_BOX_HALF_HEIGHT_SPACINGS = 0.25
        const val FILLED_DENSITY = 0.85
        const val HOLLOW_RING_RADIUS_X_SPACINGS = 0.5
        const val HOLLOW_RING_RADIUS_Y_SPACINGS = 0.36
        const val HOLLOW_HOLE_REACH_SPACINGS = 0.12
        const val HOLLOW_RING_INK_SHARE = 0.7
        const val RING_SAMPLE_COUNT = 16
        const val STEM_SIDE_OFFSET_SPACINGS = 0.55
        const val STEM_SEARCH_REACH_SPACINGS = 0.25
        const val MINIMUM_STEM_SPACINGS = 2.5
        const val STEM_NEAR_END_SPACINGS = 0.45
        const val MAXIMUM_HEAD_WIDTH_SPACINGS = 2.0
        const val HEAD_HALF_WIDTH_SPACINGS = 0.55
        const val CLUSTER_REACH_X_SPACINGS = 0.5
        const val CLUSTER_REACH_Y_SPACINGS = 0.35
        const val CHORD_WIDTH_SPACINGS = 0.6
        const val INLINE_REACH_SPACINGS = 1.8
        const val INLINE_OVERLAP_SPACINGS = 0.2
        const val ACCIDENTAL_MIN_HEIGHT_SPACINGS = 1.5
        const val ACCIDENTAL_MAX_HEIGHT_SPACINGS = 4.0
        const val ACCIDENTAL_MAX_WIDTH_SPACINGS = 1.2
        const val ACCIDENTAL_STROKE_SPACINGS = 1.2
        const val FLAT_BOWL_ABOVE_BOTTOM_SPACINGS = 0.45
        const val FLAT_LOWER_HEAVY_RATIO = 1.3
        const val KEY_ACCIDENTAL_MIN_HEIGHT_SPACINGS = 1.3
        const val KEY_ACCIDENTAL_MAX_HEIGHT_SPACINGS = 3.5
        const val STACKED_SYMBOL_MIN_HEIGHT_SPACINGS = 1.0
        const val DUPLICATE_PIXELS = 3.0
        const val CENTRE_OF_PIXEL = 0.5
        const val LETTERS_PER_OCTAVE = 7
        const val SEMITONES_PER_OCTAVE = 12

        /** Letter indexes (C = 0) of a key signature's sharps, in the order they are written. */
        val SHARP_ORDER = intArrayOf(3, 0, 4, 1, 5, 2, 6)

        /** Letter indexes (C = 0) of a key signature's flats, in the order they are written. */
        val FLAT_ORDER = intArrayOf(6, 2, 5, 1, 4, 0, 3)

        /** Semitones from C up to C, D, E, F, G, A and B. */
        val SEMITONES_ABOVE_C = intArrayOf(0, 2, 4, 5, 7, 9, 11)
    }
}
