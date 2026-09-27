package com.example.wavefret.tuner

import kotlin.math.roundToInt

/**
 * Converts a raw [TuningResult] (or its absence) from [TunerController]
 * into a [TunerDisplayState] a UI can render directly, keeping
 * cents-rounding and needle-angle math out of the Activity.
 */
class TunerDisplayFormatter {

    /**
     * Builds the display state for one tuning pipeline output.
     *
     * @param tuningResult The latest evaluated tuning result, or null if nothing usable was detected. Type: TuningResult?
     * @return The display state the tuner screen should show. Type: TunerDisplayState
     */
    fun format(tuningResult: TuningResult?): TunerDisplayState {
        if (tuningResult == null) return TunerDisplayState.Listening
        return TunerDisplayState.Detected(
            stringName = tuningResult.closestString.name,
            centsOffsetLabel = formatCentsOffsetLabel(tuningResult.centsOffset),
            status = tuningResult.status,
            needleRotationDegrees = calculateNeedleRotationDegrees(tuningResult.centsOffset)
        )
    }

    /**
     * Renders a cents offset as a signed, rounded label.
     *
     * @param centsOffset The raw cents offset from perfect pitch. Type: Double
     * @return A label such as "+7¢", "-12¢", or "0¢". Type: String
     */
    private fun formatCentsOffsetLabel(centsOffset: Double): String {
        val roundedCents = centsOffset.roundToInt()
        return if (roundedCents > 0) "+$roundedCents¢" else "$roundedCents¢"
    }

    /**
     * Maps a cents offset to a needle rotation, clamped so a wildly
     * off-pitch reading doesn't spin the needle past its visual limit.
     *
     * @param centsOffset The raw cents offset from perfect pitch. Type: Double
     * @return The rotation to apply to the needle, in degrees. Type: Float
     */
    private fun calculateNeedleRotationDegrees(centsOffset: Double): Float {
        val clampedCentsOffset = centsOffset.coerceIn(-MAX_DISPLAYED_CENTS, MAX_DISPLAYED_CENTS)
        return (clampedCentsOffset / MAX_DISPLAYED_CENTS * MAX_NEEDLE_ROTATION_DEGREES).toFloat()
    }

    private companion object {
        const val MAX_DISPLAYED_CENTS = 50.0
        const val MAX_NEEDLE_ROTATION_DEGREES = 45.0
    }
}