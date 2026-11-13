package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.LuminanceImage
import com.example.wavefret.practice.TabNote

/**
 * Reads the tab shown in a video frame. Bass covers show tabs either
 * dark on white or light on a dark background, so the frame is read with
 * the writing as dark or as light: first the way that worked last time
 * (or, at the start, the way the frame's overall brightness suggests),
 * then the other way if that finds nothing.
 *
 * @property scoreImageReader Reads the staffs and numbers. Type: ScoreImageReader
 */
class VideoFrameTabReader(private val scoreImageReader: ScoreImageReader) {

    private var lastWorkingInkIsLight: Boolean? = null

    /**
     * @param frame The frame's brightness. Type: LuminanceImage
     * @return The tab columns read in the frame, left to right; empty if it shows no tab. Type: List<List<TabNote>>
     */
    suspend fun read(frame: LuminanceImage): List<List<TabNote>> {
        val firstTry = lastWorkingInkIsLight ?: frame.isMostlyDark
        for (inkIsLight in listOf(firstTry, !firstTry)) {
            val columns = scoreImageReader.read(frame.toBinaryImage(inkIsLight)).tabSystems.flatten()
            if (columns.isNotEmpty()) {
                lastWorkingInkIsLight = inkIsLight
                return columns
            }
        }
        return emptyList()
    }
}
