package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.BinaryImage
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Reads the notes of one bass-clef staff from a page image (experimental
 * optical music recognition). It finds filled noteheads with a stem
 * (quarter notes and shorter) and hollow noteheads (half and whole notes),
 * turns their height on the staff into a pitch, applies the key signature
 * and sharps or flats written just before a note, and returns the
 * sounding pitches left to right. Bass parts are written an octave above
 * how they sound, so pitches are lowered an octave. Rhythm is not read,
 * and of notes played together only the lowest is kept.
 *
 * @property staffLineEraser Removes the staff lines before shapes are measured. Type: StaffLineEraser
 */
class NotationStaffReader(private val staffLineEraser: StaffLineEraser = StaffLineEraser()) {

    /**
     * @param image Page image. Type: BinaryImage
     * @param staff A 5-line bass-clef staff on it. Type: Staff
     * @return Sounding MIDI numbers of the staff's notes, left to right. Type: List<Int>
     */
    fun read(image: BinaryImage, staff: Staff): List<Int> {
        val spacing = staff.lineSpacing
        val bandTop = (staff.top - BAND_SPACINGS * spacing).toInt().coerceAtLeast(0)
        val bandBottom = (staff.bottom + BAND_SPACINGS * spacing).toInt().coerceAtMost(image.height - 1)
        val working = staffLineEraser.erase(image, listOf(staff), eraseBarLines = false)
        eraseLedgerLines(working, staff, bandTop, bandBottom)
        val shapes = ShapeMeasurements(working, staff.left, staff.right, bandTop, bandBottom)

        val firstNoteColumn = staff.left + CLEF_WIDTH_SPACINGS * spacing
        val noteheads = (findFilledNoteheads(shapes, staff) + findHollowNoteheads(shapes, staff))
            .filter { it.centreX >= firstNoteColumn }
            .let(::withoutDuplicates)
            .sortedBy { it.centreX }
        if (noteheads.isEmpty()) return emptyList()

        val accidentals = findAccidentals(working, staff, bandTop, bandBottom, noteheads)
        val inlineAccidentals = noteheads.associateWith { notehead ->
            accidentals.firstOrNull { accidental ->
                accidental.right <= notehead.left + spacing * INLINE_OVERLAP_SPACINGS &&
                    notehead.left - accidental.right <= spacing * INLINE_REACH_SPACINGS &&
                    abs(accidental.position - positionOf(notehead.centreY, staff)) <= 1
            }
        }
        val usedInline = inlineAccidentals.values.filterNotNull().toSet()
        val keySignature = accidentals
            .filter { it !in usedInline && it.right < noteheads.first().left }
            .associate { letterIndexOf(it.position) to if (it.isFlat) -1 else 1 }

        val columns = mutableListOf<MutableList<Notehead>>()
        noteheads.forEach { notehead ->
            val column = columns.lastOrNull()
            if (column != null && notehead.centreX - column.first().centreX <= spacing * CHORD_WIDTH_SPACINGS) column += notehead
            else columns += mutableListOf(notehead)
        }
        return columns.map { column ->
            column.minOf { notehead ->
                val position = positionOf(notehead.centreY, staff)
                val letterIndex = letterIndexOf(position)
                val keyAlteration = keySignature[letterIndex] ?: 0
                val alteration = when (val accidental = inlineAccidentals[notehead]) {
                    null -> keyAlteration
                    else -> if (accidental.isFlat) -1 else if (keyAlteration < 0) 0 else 1
                }
                writtenNaturalMidiNumber(position) + alteration - WRITTEN_OCTAVE_ABOVE_SOUNDING
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
            .filter { notehead -> hasStem(shapes, notehead, spacing) }
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
     * @return True if a long vertical stroke touches its left or right side. Type: Boolean
     */
    private fun hasStem(shapes: ShapeMeasurements, notehead: Notehead, spacing: Double): Boolean {
        val sideOffset = (spacing * STEM_SIDE_OFFSET_SPACINGS).roundToInt()
        val reach = (spacing * STEM_SEARCH_REACH_SPACINGS).roundToInt()
        val minimumStem = spacing * MINIMUM_STEM_SPACINGS
        val centreX = notehead.centreX.roundToInt()
        val centreY = notehead.centreY.roundToInt()
        val columns = (centreX - sideOffset - reach..centreX - sideOffset + reach) + (centreX + sideOffset - reach..centreX + sideOffset + reach)
        return columns.any { x ->
            (centreY - reach..centreY + reach).any { y -> verticalRunAt(shapes.image, x, y) >= minimumStem }
        }
    }

    /**
     * @param working Image without staff lines. Type: BinaryImage
     * @param staff The staff. Type: Staff
     * @param bandTop First row searched. Type: Int
     * @param bandBottom Last row searched. Type: Int
     * @param noteheads Noteheads found, whose shapes are not accidentals. Type: List<Notehead>
     * @return Sharps, naturals and flats found on the staff. Type: List<Accidental>
     */
    private fun findAccidentals(
        working: BinaryImage,
        staff: Staff,
        bandTop: Int,
        bandBottom: Int,
        noteheads: List<Notehead>
    ): List<Accidental> {
        val spacing = staff.lineSpacing
        val firstAccidentalColumn = staff.left + KEY_SIGNATURE_START_SPACINGS * spacing
        return connectedShapes(working, staff.left, staff.right, bandTop, bandBottom)
            .filter { shape ->
                val shapeHeight = shape.bottom - shape.top + 1
                val shapeWidth = shape.right - shape.left + 1
                shape.left >= firstAccidentalColumn &&
                    shapeHeight >= spacing * ACCIDENTAL_MIN_HEIGHT_SPACINGS && shapeHeight <= spacing * ACCIDENTAL_MAX_HEIGHT_SPACINGS &&
                    shapeWidth <= spacing * ACCIDENTAL_MAX_WIDTH_SPACINGS &&
                    noteheads.none { it.centreX.roundToInt() in shape.left..shape.right && it.centreY.roundToInt() in shape.top..shape.bottom }
            }
            .mapNotNull { shape ->
                val strokeCount = verticalStrokeCount(working, shape, spacing)
                when {
                    strokeCount == 1 -> Accidental(
                        isFlat = true,
                        position = positionOf(shape.bottom + 1 - spacing * FLAT_BOWL_ABOVE_BOTTOM_SPACINGS, staff),
                        right = shape.right
                    )
                    strokeCount >= 2 -> Accidental(
                        isFlat = false,
                        position = positionOf((shape.top + shape.bottom + 1) / 2.0, staff),
                        right = shape.right
                    )
                    else -> null
                }
            }
    }

    /**
     * @param image Image without staff lines. Type: BinaryImage
     * @param shape A connected shape. Type: Shape
     * @param spacing Staff line spacing. Type: Double
     * @return Number of separate tall vertical strokes in the shape (1 for a flat, 2 for a sharp or natural). Type: Int
     */
    private fun verticalStrokeCount(image: BinaryImage, shape: Shape, spacing: Double): Int {
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
     * @param image Image to label. Type: BinaryImage
     * @param left Leftmost column. Type: Int
     * @param right Rightmost column. Type: Int
     * @param top Top row. Type: Int
     * @param bottom Bottom row. Type: Int
     * @return Bounding boxes of the connected ink shapes in the region. Type: List<Shape>
     */
    private fun connectedShapes(image: BinaryImage, left: Int, right: Int, top: Int, bottom: Int): List<Shape> {
        val regionWidth = right - left + 1
        val visited = BooleanArray(regionWidth * (bottom - top + 1))
        val shapes = mutableListOf<Shape>()
        val stack = ArrayDeque<Int>()
        for (startY in top..bottom) for (startX in left..right) {
            val startIndex = (startY - top) * regionWidth + (startX - left)
            if (visited[startIndex] || !image.isInk(startX, startY)) continue
            var shape = Shape(startX, startY, startX, startY)
            visited[startIndex] = true
            stack.addLast(startIndex)
            while (stack.isNotEmpty()) {
                val index = stack.removeLast()
                val x = index % regionWidth + left
                val y = index / regionWidth + top
                shape = Shape(minOf(shape.left, x), minOf(shape.top, y), maxOf(shape.right, x), maxOf(shape.bottom, y))
                for ((dx, dy) in NEIGHBOURS) {
                    val nextX = x + dx
                    val nextY = y + dy
                    if (nextX !in left..right || nextY !in top..bottom) continue
                    val nextIndex = (nextY - top) * regionWidth + (nextX - left)
                    if (!visited[nextIndex] && image.isInk(nextX, nextY)) {
                        visited[nextIndex] = true
                        stack.addLast(nextIndex)
                    }
                }
            }
            shapes += shape
        }
        return shapes
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
    private fun verticalRunAt(image: BinaryImage, x: Int, y: Int): Int {
        if (!image.isInk(x, y)) return 0
        var top = y
        while (image.isInk(x, top - 1)) top--
        var bottom = y
        while (image.isInk(x, bottom + 1)) bottom++
        return bottom - top + 1
    }

    /**
     * @param centreY Vertical centre of a notehead or accidental. Type: Double
     * @param staff The staff. Type: Staff
     * @return Staff position: 0 is the bottom line, each step a line or space higher. Type: Int
     */
    private fun positionOf(centreY: Double, staff: Staff): Int = ((staff.bottom - centreY) / (staff.lineSpacing / 2)).roundToInt()

    /**
     * @param position Staff position. Type: Int
     * @return Letter index from G (0) to F (6) in bass clef. Type: Int
     */
    private fun letterIndexOf(position: Int): Int = Math.floorMod(position, LETTERS_PER_OCTAVE)

    /**
     * @param position Staff position in bass clef. Type: Int
     * @return Written MIDI number of the natural note there (the bottom line is G2, 43). Type: Int
     */
    private fun writtenNaturalMidiNumber(position: Int): Int {
        val octaves = Math.floorDiv(position, LETTERS_PER_OCTAVE)
        return BOTTOM_LINE_MIDI_NUMBER + octaves * SEMITONES_PER_OCTAVE + SEMITONES_ABOVE_G[letterIndexOf(position)]
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

    /**
     * Bounding box of a connected ink shape.
     *
     * @property left Leftmost column. Type: Int
     * @property top Top row. Type: Int
     * @property right Rightmost column. Type: Int
     * @property bottom Bottom row. Type: Int
     */
    private data class Shape(val left: Int, val top: Int, val right: Int, val bottom: Int)

    private companion object {
        const val BAND_SPACINGS = 5.0
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
        const val DUPLICATE_PIXELS = 3.0
        const val CENTRE_OF_PIXEL = 0.5
        const val LETTERS_PER_OCTAVE = 7
        const val SEMITONES_PER_OCTAVE = 12
        const val BOTTOM_LINE_MIDI_NUMBER = 43
        const val WRITTEN_OCTAVE_ABOVE_SOUNDING = 12

        /** Semitones from G up to G, A, B, C, D, E and F. */
        val SEMITONES_ABOVE_G = intArrayOf(0, 2, 4, 5, 7, 9, 10)
        val NEIGHBOURS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, -1 to -1, 1 to -1, -1 to 1)
    }
}
