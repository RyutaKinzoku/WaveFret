package com.example.wavefret.common.transcription

import com.example.wavefret.common.music.NoteEvent
import kotlin.math.roundToLong

/**
 * Turns basic-pitch's frame-by-pitch probability maps into notes: each
 * local peak in the onset map starts a note, which lasts while the note map
 * stays above a threshold (tolerating brief dips), until the next onset on
 * the same pitch. A simplified version of basic-pitch's own decoding.
 *
 * @property onsetThreshold Onset probability needed to start a note. Type: Float
 * @property frameThreshold Note probability needed to keep it sounding. Type: Float
 * @property minimumNoteFrames Shorter notes are discarded as noise. Type: Int
 * @property gapToleranceFrames Dips this long or shorter don't end a note. Type: Int
 * @property framesPerSecond Frame rate of the maps. Type: Double
 */
class PosteriorgramNoteDecoder(
    private val onsetThreshold: Float = DEFAULT_ONSET_THRESHOLD,
    private val frameThreshold: Float = DEFAULT_FRAME_THRESHOLD,
    private val minimumNoteFrames: Int = DEFAULT_MINIMUM_NOTE_FRAMES,
    private val gapToleranceFrames: Int = DEFAULT_GAP_TOLERANCE_FRAMES,
    private val framesPerSecond: Double = BasicPitchWindowing.FRAMES_PER_SECOND
) {

    /**
     * @param noteFrames Probability each pitch is sounding, frames by 88 bins (A0 first). Type: Array<FloatArray>
     * @param onsetFrames Probability each pitch starts, same shape. Type: Array<FloatArray>
     * @return Detected notes in time order. Type: List<NoteEvent>
     */
    fun decode(noteFrames: Array<FloatArray>, onsetFrames: Array<FloatArray>): List<NoteEvent> {
        val frameCount = minOf(noteFrames.size, onsetFrames.size)
        val onsetPeaks = mutableListOf<Pair<Int, Int>>()
        for (frame in 0 until frameCount) {
            for (bin in onsetFrames[frame].indices) {
                if (isOnsetPeak(onsetFrames, frame, bin, frameCount)) onsetPeaks += frame to bin
            }
        }
        val onsetFramesByBin = onsetPeaks.groupBy({ it.second }, { it.first })
        return onsetPeaks.mapNotNull { (onsetFrame, bin) ->
            val nextOnsetFrame = onsetFramesByBin.getValue(bin).firstOrNull { it > onsetFrame } ?: frameCount
            val lastActiveFrame = lastActiveFrame(noteFrames, bin, onsetFrame, nextOnsetFrame)
            val noteFrameCount = lastActiveFrame - onsetFrame + 1
            if (noteFrameCount < minimumNoteFrames) null
            else NoteEvent(
                midiNumber = bin + LOWEST_MIDI_NUMBER,
                onsetMillis = millisAt(onsetFrame),
                durationMillis = millisAt(lastActiveFrame + 1) - millisAt(onsetFrame)
            )
        }.sortedWith(compareBy({ it.onsetMillis }, { it.midiNumber }))
    }

    /**
     * @param onsetFrames Onset map. Type: Array<FloatArray>
     * @param frame Frame to check. Type: Int
     * @param bin Pitch bin to check. Type: Int
     * @param frameCount Frames in the map. Type: Int
     * @return True if this is a confident local peak in time. Type: Boolean
     */
    private fun isOnsetPeak(onsetFrames: Array<FloatArray>, frame: Int, bin: Int, frameCount: Int): Boolean {
        val value = onsetFrames[frame][bin]
        if (value < onsetThreshold) return false
        val previous = if (frame > 0) onsetFrames[frame - 1][bin] else 0f
        val next = if (frame + 1 < frameCount) onsetFrames[frame + 1][bin] else 0f
        return value > previous && value >= next
    }

    /**
     * @param noteFrames Note map. Type: Array<FloatArray>
     * @param bin Pitch bin. Type: Int
     * @param onsetFrame Where the note starts. Type: Int
     * @param stopFrame First frame belonging to the next note on this pitch. Type: Int
     * @return Last frame the note is still sounding. Type: Int
     */
    private fun lastActiveFrame(noteFrames: Array<FloatArray>, bin: Int, onsetFrame: Int, stopFrame: Int): Int {
        var lastActiveFrame = onsetFrame
        var gapFrames = 0
        for (frame in onsetFrame + 1 until stopFrame) {
            if (noteFrames[frame][bin] >= frameThreshold) {
                lastActiveFrame = frame
                gapFrames = 0
            } else if (++gapFrames > gapToleranceFrames) {
                break
            }
        }
        return lastActiveFrame
    }

    /**
     * @param frame Frame index. Type: Int
     * @return Its start time, in milliseconds. Type: Long
     */
    private fun millisAt(frame: Int): Long = (frame * MILLIS_PER_SECOND / framesPerSecond).roundToLong()

    private companion object {
        const val LOWEST_MIDI_NUMBER = 21
        const val MILLIS_PER_SECOND = 1_000.0
        const val DEFAULT_ONSET_THRESHOLD = 0.5f
        const val DEFAULT_FRAME_THRESHOLD = 0.3f
        const val DEFAULT_MINIMUM_NOTE_FRAMES = 6
        const val DEFAULT_GAP_TOLERANCE_FRAMES = 11
    }
}
