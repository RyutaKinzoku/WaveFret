package com.example.wavefret.practice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.google.android.material.color.MaterialColors

/**
 * Draws the exercise as a horizontally scrolling bass tab: one line per
 * string, a fixed playhead, and each note as a fret number colored by its
 * [TabNoteStatus]. All geometry comes from the tested [TabStaffLayout];
 * this class only paints, so it is verified on-device.
 *
 * In tempo mode a [playheadPositionProvider] is set and the view redraws
 * every frame from it, so scrolling is smooth between audio buffers.
 */
class TabStaffView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : View(context, attributeSet) {

    private val density = resources.displayMetrics.density
    private val stringLinePaint = strokePaint(com.google.android.material.R.attr.colorOutlineVariant, STRING_LINE_WIDTH_DP)
    private val playheadPaint = strokePaint(androidx.appcompat.R.attr.colorPrimary, PLAYHEAD_WIDTH_DP)
    private val noteBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val fretNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, FRET_TEXT_SIZE_SP, resources.displayMetrics)
        isFakeBoldText = true
    }

    private var exercise: Exercise? = null
    private var displayState: PracticeDisplayState? = null
    private var tabStaffLayout: TabStaffLayout? = null

    /** When set, supplies the live scroll position each frame instead of the last state. Type: (() -> Double)? */
    var playheadPositionProvider: (() -> Double)? = null
        set(value) {
            field = value
            invalidate()
        }

    /**
     * @param exerciseToShow Exercise whose notes are drawn. Type: Exercise
     * @param latestDisplayState Latest statuses and scroll position. Type: PracticeDisplayState
     * @return Unit
     */
    fun render(exerciseToShow: Exercise, latestDisplayState: PracticeDisplayState) {
        if (exercise?.tuning != exerciseToShow.tuning) tabStaffLayout = null
        exercise = exerciseToShow
        displayState = latestDisplayState
        invalidate()
    }

    /**
     * Drops the cached geometry so it is rebuilt for the new size.
     *
     * @param newWidth New view width, in px. Type: Int
     * @param newHeight New view height, in px. Type: Int
     * @param oldWidth Previous view width, in px. Type: Int
     * @param oldHeight Previous view height, in px. Type: Int
     * @return Unit
     */
    override fun onSizeChanged(newWidth: Int, newHeight: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight)
        tabStaffLayout = null
    }

    /**
     * Builds the staff geometry for the current size and string count; kept
     * out of onDraw so drawing a frame allocates nothing.
     *
     * @param stringCount Strings in the tuning being shown. Type: Int
     * @return Geometry for the current view size. Type: TabStaffLayout
     */
    private fun buildTabStaffLayout(stringCount: Int): TabStaffLayout {
        val stringSpacingPx = (height - 2 * VERTICAL_PADDING_DP * density) / (stringCount - 1).coerceAtLeast(1)
        return TabStaffLayout(
            noteSpacingPx = NOTE_SPACING_DP * density,
            playheadXPx = width * PLAYHEAD_FRACTION_OF_WIDTH,
            topPaddingPx = VERTICAL_PADDING_DP * density,
            stringSpacingPx = stringSpacingPx
        )
    }

    /**
     * @param canvas Canvas to draw the staff on. Type: Canvas
     * @return Unit
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val shownExercise = exercise ?: return
        val shownState = displayState ?: return
        val stringCount = shownExercise.tuning.strings.size
        val tabStaffLayout = tabStaffLayout ?: buildTabStaffLayout(stringCount).also { tabStaffLayout = it }
        val playheadPosition = playheadPositionProvider?.invoke() ?: shownState.playheadPosition

        for (stringIndex in 0 until stringCount) {
            val stringLineY = tabStaffLayout.stringLineYPx(stringIndex, stringCount)
            canvas.drawLine(0f, stringLineY, width.toFloat(), stringLineY, stringLinePaint)
        }
        val playheadX = width * PLAYHEAD_FRACTION_OF_WIDTH
        canvas.drawLine(playheadX, 0f, playheadX, height.toFloat(), playheadPaint)

        for (noteIndex in tabStaffLayout.visibleNoteIndices(shownExercise.noteCount, playheadPosition, width.toFloat())) {
            val tabNote = shownExercise.notes[noteIndex]
            drawNote(
                canvas = canvas,
                centerX = tabStaffLayout.noteCenterXPx(noteIndex, playheadPosition),
                centerY = tabStaffLayout.stringLineYPx(tabNote.stringIndex, stringCount),
                fret = tabNote.fret,
                tabNoteStatus = shownState.noteStatuses[noteIndex]
            )
        }
        if (playheadPositionProvider != null) postInvalidateOnAnimation()
    }

    /**
     * @param canvas Canvas to draw on. Type: Canvas
     * @param centerX Horizontal center of the note. Type: Float
     * @param centerY Vertical center of the note (its string line). Type: Float
     * @param fret Fret number to print. Type: Int
     * @param tabNoteStatus Decides the note's color. Type: TabNoteStatus
     * @return Unit
     */
    private fun drawNote(canvas: Canvas, centerX: Float, centerY: Float, fret: Int, tabNoteStatus: TabNoteStatus) {
        noteBackgroundPaint.color = MaterialColors.getColor(this, backgroundColorAttributeFor(tabNoteStatus))
        val radius = (if (tabNoteStatus == TabNoteStatus.CURRENT) CURRENT_NOTE_RADIUS_DP else NOTE_RADIUS_DP) * density
        canvas.drawCircle(centerX, centerY, radius, noteBackgroundPaint)
        fretNumberPaint.color = MaterialColors.getColor(this, textColorAttributeFor(tabNoteStatus))
        val baselineY = centerY - (fretNumberPaint.descent() + fretNumberPaint.ascent()) / 2
        canvas.drawText(fret.toString(), centerX, baselineY, fretNumberPaint)
    }

    /**
     * @param tabNoteStatus Status of a note. Type: TabNoteStatus
     * @return Theme colour role for that note's background. Type: Int
     */
    private fun backgroundColorAttributeFor(tabNoteStatus: TabNoteStatus): Int = when (tabNoteStatus) {
        TabNoteStatus.HIT -> com.google.android.material.R.attr.colorSecondary
        TabNoteStatus.MISSED -> androidx.appcompat.R.attr.colorError
        TabNoteStatus.CURRENT -> androidx.appcompat.R.attr.colorPrimary
        TabNoteStatus.UPCOMING -> com.google.android.material.R.attr.colorSurfaceContainerHighest
    }

    /**
     * @param tabNoteStatus Status of a note. Type: TabNoteStatus
     * @return Theme colour role for that note's fret number, contrasting with its background. Type: Int
     */
    private fun textColorAttributeFor(tabNoteStatus: TabNoteStatus): Int = when (tabNoteStatus) {
        TabNoteStatus.HIT -> com.google.android.material.R.attr.colorOnSecondary
        TabNoteStatus.MISSED -> com.google.android.material.R.attr.colorOnError
        TabNoteStatus.CURRENT -> com.google.android.material.R.attr.colorOnPrimary
        TabNoteStatus.UPCOMING -> com.google.android.material.R.attr.colorOnSurface
    }

    /**
     * @param colorAttribute Theme colour role for the stroke. Type: Int
     * @param strokeWidthDp Stroke width, in dp. Type: Float
     * @return A configured stroke paint. Type: Paint
     */
    private fun strokePaint(colorAttribute: Int, strokeWidthDp: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(this@TabStaffView, colorAttribute)
        strokeWidth = strokeWidthDp * density
        style = Paint.Style.STROKE
    }

    private companion object {
        const val NOTE_SPACING_DP = 72f
        const val NOTE_RADIUS_DP = 16f
        const val CURRENT_NOTE_RADIUS_DP = 20f
        const val VERTICAL_PADDING_DP = 24f
        const val STRING_LINE_WIDTH_DP = 2f
        const val PLAYHEAD_WIDTH_DP = 3f
        const val FRET_TEXT_SIZE_SP = 16f
        const val PLAYHEAD_FRACTION_OF_WIDTH = 0.25f
    }
}
