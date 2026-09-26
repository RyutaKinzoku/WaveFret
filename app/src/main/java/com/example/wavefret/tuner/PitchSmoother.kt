package com.example.wavefret.tuner

/**
 * Smooths a live stream of pitch readings by taking the median of the last
 * few readings, so a single spurious reading (an octave jump, a stray
 * noise spike) doesn't make the displayed pitch jump around. Median is
 * used rather than a moving average specifically because it ignores a
 * single outlier entirely, instead of averaging it in.
 *
 * @property windowSize How many recent readings to keep and take the
 *   median of. Type: Int
 */
class PitchSmoother(private val windowSize: Int = 5) {

    private val recentReadings = ArrayDeque<Double>()

    /**
     * Records a new pitch reading and returns the current smoothed value.
     *
     * @param frequencyHz Latest raw pitch reading, in Hz. Type: Double
     * @return Median of the last windowSize readings (including this one). Type: Double
     */
    fun addReading(frequencyHz: Double): Double {
        recentReadings.addLast(frequencyHz)
        if (recentReadings.size > windowSize) {
            recentReadings.removeFirst()
        }
        return median(recentReadings)
    }

    /**
     * @param values Readings to find the median of. Type: List<Double>
     * @return The median value, averaging the two middle values when the
     *   count is even. Type: Double
     */
    private fun median(values: List<Double>): Double {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }
}