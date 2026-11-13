package com.example.wavefret.common.imaging

/**
 * Chooses the luminance that best separates ink from paper in an image
 * (Otsu's method), so photos with grey paper or faint ink still turn into
 * clean black and white.
 */
object InkThreshold {

    private const val LEVELS = 256
    private const val LENIENT_SHARE_OF_PAPER_GAP = 4

    /**
     * @param luminance Luminance of every pixel, 0 black to 255 white. Type: IntArray
     * @return Luminance below which a pixel is ink; the default when the image has only one tone. Type: Int
     */
    fun of(luminance: IntArray): Int {
        val histogram = IntArray(LEVELS)
        luminance.forEach { histogram[it.coerceIn(0, LEVELS - 1)]++ }
        val total = luminance.size.toDouble()
        val weightedTotal = (0 until LEVELS).sumOf { it.toDouble() * histogram[it] }
        var darkCount = 0.0
        var darkWeighted = 0.0
        var bestVariance = 0.0
        var bestLevel = -1
        for (level in 0 until LEVELS) {
            darkCount += histogram[level]
            if (darkCount == 0.0) continue
            val lightCount = total - darkCount
            if (lightCount == 0.0) break
            darkWeighted += level.toDouble() * histogram[level]
            val darkMean = darkWeighted / darkCount
            val lightMean = (weightedTotal - darkWeighted) / lightCount
            val betweenClassVariance = darkCount * lightCount * (darkMean - lightMean) * (darkMean - lightMean)
            if (betweenClassVariance > bestVariance) {
                bestVariance = betweenClassVariance
                bestLevel = level
            }
        }
        return if (bestLevel < 0) BinaryImage.DEFAULT_INK_THRESHOLD else bestLevel + 1
    }

    /**
     * A threshold close to the paper's own tone, so faint grey lines
     * (anti-aliased or photographed staff lines) still count as ink while
     * the paper does not.
     *
     * @param luminance Luminance of every pixel, 0 black to 255 white; most pixels are paper. Type: IntArray
     * @return Luminance below which a pixel is ink. Type: Int
     */
    fun lenient(luminance: IntArray): Int {
        val paperLevel = luminance.sorted()[luminance.size / 2]
        val inkLevel = of(luminance)
        return (paperLevel - (paperLevel - inkLevel) / LENIENT_SHARE_OF_PAPER_GAP).coerceAtLeast(inkLevel)
    }
}
