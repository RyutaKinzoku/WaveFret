package com.example.wavefret.common.transcription

/**
 * Which of the model's two 88-bin outputs is which. The TFLite file doesn't
 * name them, so they are told apart by content: onsets are brief spikes, so
 * the onset map is the sparser (lower average) one.
 *
 * @property notes Probability each pitch is sounding. Type: Array<FloatArray>
 * @property onsets Probability each pitch starts. Type: Array<FloatArray>
 */
class BasicPitchOutputRoles private constructor(
    val notes: Array<FloatArray>,
    val onsets: Array<FloatArray>
) {
    companion object {
        /**
         * @param firstMap One 88-bin output. Type: Array<FloatArray>
         * @param secondMap The other 88-bin output. Type: Array<FloatArray>
         * @return The two maps with their roles. Type: BasicPitchOutputRoles
         */
        fun assign(firstMap: Array<FloatArray>, secondMap: Array<FloatArray>): BasicPitchOutputRoles {
            return if (meanOf(firstMap) <= meanOf(secondMap)) {
                BasicPitchOutputRoles(notes = secondMap, onsets = firstMap)
            } else {
                BasicPitchOutputRoles(notes = firstMap, onsets = secondMap)
            }
        }

        /**
         * @param map Frames-by-bins map. Type: Array<FloatArray>
         * @return Mean value over all cells. Type: Double
         */
        private fun meanOf(map: Array<FloatArray>): Double {
            val cellCount = map.sumOf { it.size }
            return if (cellCount == 0) 0.0 else map.sumOf { frame -> frame.sumOf { it.toDouble() } } / cellCount
        }
    }
}
