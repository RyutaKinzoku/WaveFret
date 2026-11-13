package com.example.wavefret.practice.songimport

import com.example.wavefret.common.textrecognition.RecognizedSymbol
import com.example.wavefret.common.textrecognition.TextGridBuilder

/**
 * Turns the symbols recognized on each page of a photo or PDF into tab
 * text the [AsciiTabParser] can read: each page is rebuilt as monospaced
 * text, the missing staff lines are filled back in, and pages are joined
 * in order.
 *
 * @property textGridBuilder Rebuilds monospaced text from symbol positions. Type: TextGridBuilder
 * @property tabLineGapFiller Restores dashes on tab lines. Type: TabLineGapFiller
 */
class RecognizedTabTextBuilder(
    private val textGridBuilder: TextGridBuilder = TextGridBuilder(),
    private val tabLineGapFiller: TabLineGapFiller = TabLineGapFiller()
) {

    /**
     * @param pages Symbols recognized on each page, first page first. Type: List<List<RecognizedSymbol>>
     * @return The tab text of all pages, separated by blank lines. Type: String
     */
    fun build(pages: List<List<RecognizedSymbol>>): String {
        return pages
            .map { tabLineGapFiller.fill(textGridBuilder.build(it)) }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
    }
}
