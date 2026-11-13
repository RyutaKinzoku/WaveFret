package com.example.wavefret.practice.songimport

import com.example.wavefret.common.music.BassTuning
import com.example.wavefret.practice.TabNote

/**
 * Writes notes as a plain ASCII bass tab (G|-----|), so a tab read from a
 * picture can be shown, checked and edited like a pasted one.
 *
 * @property tuning Tuning whose string names head the lines. Type: BassTuning
 */
class AsciiTabWriter(private val tuning: BassTuning) {

    /**
     * @param systems Systems in order; each is a list of columns of notes played together. Type: List<List<List<TabNote>>>
     * @return The tab text, systems separated by a blank line. Type: String
     */
    fun write(systems: List<List<List<TabNote>>>): String {
        return systems.joinToString("\n\n") { columns ->
            tuning.strings.indices.reversed().joinToString("\n") { stringIndex ->
                val line = StringBuilder(tuning.strings[stringIndex].name).append(LINE_START)
                columns.forEach { column ->
                    val columnWidth = column.maxOf { it.fret.toString().length }
                    val fretText = column.firstOrNull { it.stringIndex == stringIndex }?.fret?.toString().orEmpty()
                    line.append(fretText.padEnd(columnWidth, '-')).append('-')
                }
                line.append(LINE_END).toString()
            }
        }
    }

    private companion object {
        /** Padding so even a one-note line has enough dashes to be read back as tab. */
        const val LINE_START = "|--"
        const val LINE_END = "-|"
    }
}
