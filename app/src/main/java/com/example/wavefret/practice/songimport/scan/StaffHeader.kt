package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.common.imaging.PixelRegion

/**
 * The symbols at the start of a music staff, before its first note.
 *
 * @property clef Bounding box of the clef. Type: PixelRegion
 * @property symbols Bounding boxes of what follows the clef: key signature, time signature, the clef's dots. Type: List<PixelRegion>
 * @property endColumn Rightmost column of the header; notes start after it. Type: Int
 */
data class StaffHeader(val clef: PixelRegion, val symbols: List<PixelRegion>, val endColumn: Int)
