package com.example.wavefret.tuner

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.google.android.material.color.MaterialColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Semicircular tuning gauge: a scale from -50 to +50 cents with a tick
 * every 10 cents, a highlighted in-tune zone in the middle, and a needle
 * that glides between readings (which arrive about 40 times a second). The
 * needle angle comes from the tested [TunerDisplayFormatter]; this class
 * only paints, so it is verified on-device.
 */
class TunerGaugeView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : View(context, attributeSet) {

    private val density = resources.displayMetrics.density
    private val scalePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = SCALE_WIDTH_DP * density
        strokeCap = Paint.Cap.ROUND
        color = MaterialColors.getColor(this@TunerGaugeView, com.google.android.material.R.attr.colorSurfaceContainerHighest)
    }
    private val inTuneZonePaint = Paint(scalePaint).apply {
        color = MaterialColors.getColor(this@TunerGaugeView, com.google.android.material.R.attr.colorSecondary)
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = TICK_WIDTH_DP * density
        strokeCap = Paint.Cap.ROUND
        color = MaterialColors.getColor(this@TunerGaugeView, com.google.android.material.R.attr.colorOutline)
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = NEEDLE_WIDTH_DP * density
        strokeCap = Paint.Cap.ROUND
    }
    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scaleBounds = RectF()

    private var needleRotationDegrees = 0f
    private var needleColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline)
    private var needleAnimator: ValueAnimator? = null

    /**
     * Glides the needle to a new angle and recolours it.
     *
     * @param targetRotationDegrees Needle angle, negative is flat, 0 is in tune. Type: Float
     * @param isInTune Whether to draw the needle in the in-tune colour. Type: Boolean
     * @param isActive False while nothing is heard, which greys the needle out. Type: Boolean
     * @return Unit
     */
    fun showReading(targetRotationDegrees: Float, isInTune: Boolean, isActive: Boolean) {
        val colorAttribute = when {
            !isActive -> com.google.android.material.R.attr.colorOutline
            isInTune -> com.google.android.material.R.attr.colorSecondary
            else -> androidx.appcompat.R.attr.colorPrimary
        }
        needleColor = MaterialColors.getColor(this, colorAttribute)
        needleAnimator?.cancel()
        needleAnimator = ValueAnimator.ofFloat(needleRotationDegrees, targetRotationDegrees).apply {
            duration = NEEDLE_ANIMATION_MILLIS
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                needleRotationDegrees = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /**
     * @param newWidth New view width, in px. Type: Int
     * @param newHeight New view height, in px. Type: Int
     * @param oldWidth Previous view width, in px. Type: Int
     * @param oldHeight Previous view height, in px. Type: Int
     * @return Unit
     */
    override fun onSizeChanged(newWidth: Int, newHeight: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight)
        val radius = min(newWidth / 2f, newHeight.toFloat()) - SCALE_WIDTH_DP * density
        val centerX = newWidth / 2f
        val centerY = newHeight - SCALE_WIDTH_DP * density
        scaleBounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
    }

    /**
     * @param canvas Canvas to draw the gauge on. Type: Canvas
     * @return Unit
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = scaleBounds.centerX()
        val centerY = scaleBounds.centerY()
        val radius = scaleBounds.width() / 2f

        canvas.drawArc(scaleBounds, ARC_START_DEGREES, ARC_SWEEP_DEGREES, false, scalePaint)
        canvas.drawArc(
            scaleBounds, UPRIGHT_DEGREES - IN_TUNE_ZONE_HALF_DEGREES, 2 * IN_TUNE_ZONE_HALF_DEGREES, false, inTuneZonePaint
        )

        for (tickIndex in -TICKS_PER_SIDE..TICKS_PER_SIDE) {
            val tickAngle = Math.toRadians((UPRIGHT_DEGREES + tickIndex * DEGREES_PER_TICK).toDouble())
            val tickLength = (if (tickIndex == 0) MAJOR_TICK_DP else MINOR_TICK_DP) * density
            val innerRadius = radius - SCALE_WIDTH_DP * density - tickLength
            val outerRadius = radius - SCALE_WIDTH_DP * density
            canvas.drawLine(
                centerX + innerRadius * cos(tickAngle).toFloat(), centerY + innerRadius * sin(tickAngle).toFloat(),
                centerX + outerRadius * cos(tickAngle).toFloat(), centerY + outerRadius * sin(tickAngle).toFloat(),
                tickPaint
            )
        }

        val needleAngle = Math.toRadians((UPRIGHT_DEGREES + needleRotationDegrees).toDouble())
        val needleLength = radius - NEEDLE_INSET_DP * density
        needlePaint.color = needleColor
        hubPaint.color = needleColor
        canvas.drawLine(
            centerX, centerY,
            centerX + needleLength * cos(needleAngle).toFloat(), centerY + needleLength * sin(needleAngle).toFloat(),
            needlePaint
        )
        canvas.drawCircle(centerX, centerY, HUB_RADIUS_DP * density, hubPaint)
    }

    /**
     * Stops any running needle animation when the view leaves the screen.
     *
     * @return Unit
     */
    override fun onDetachedFromWindow() {
        needleAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    private companion object {
        /** Needle range from TunerDisplayFormatter: ±45° spans ±50 cents. */
        const val DEGREES_PER_TICK = 9f
        const val TICKS_PER_SIDE = 5
        const val UPRIGHT_DEGREES = 270f
        const val ARC_START_DEGREES = 270f - 50f
        const val ARC_SWEEP_DEGREES = 100f
        const val IN_TUNE_ZONE_HALF_DEGREES = 4.5f
        const val SCALE_WIDTH_DP = 8f
        const val TICK_WIDTH_DP = 2f
        const val MINOR_TICK_DP = 8f
        const val MAJOR_TICK_DP = 16f
        const val NEEDLE_WIDTH_DP = 4f
        const val NEEDLE_INSET_DP = 28f
        const val HUB_RADIUS_DP = 8f
        /** About one reading interval, so the needle glides continuously between readings. */
        const val NEEDLE_ANIMATION_MILLIS = 40L
    }
}
