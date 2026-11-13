package com.example.wavefret.practice.songimport.scan

import com.example.wavefret.practice.TabNote

/**
 * What was read from one page image.
 *
 * @property tabSystems Each tab staff's notes as columns, top staff first. Type: List<List<List<TabNote>>>
 * @property notationSystems Each music staff's sounding MIDI numbers, top staff first. Type: List<List<Int>>
 */
data class PageReading(
    val tabSystems: List<List<List<TabNote>>>,
    val notationSystems: List<List<Int>>
)
