package com.example.wavefret.practice.songimport

/**
 * Repairs tab lines read from a photo or PDF, where text recognition
 * usually sees the fret numbers but not the staff line between them:
 * on a line that looks like tab (a bar or dash after an optional string
 * name), every gap is turned back into dashes. Other lines are untouched.
 */
class TabLineGapFiller {

    /**
     * @param text Recognized text, possibly several lines. Type: String
     * @return The text with gaps inside tab lines filled with dashes. Type: String
     */
    fun fill(text: String): String {
        return text.lines().joinToString("\n", transform = ::fillLine)
    }

    /**
     * @param line One line of text. Type: String
     * @return The line, filled if it is a tab line. Type: String
     */
    private fun fillLine(line: String): String {
        val lineMatch = TAB_LINE_PATTERN.matchEntire(line) ?: return line
        val prefix = lineMatch.groupValues[1]
        val content = lineMatch.groupValues[2].trimEnd()
        return prefix + content.replace(' ', '-')
    }

    private companion object {
        /** Optional indentation and string name, then content starting with a bar or dash. */
        val TAB_LINE_PATTERN = Regex("^(\\s*(?:[A-Ga-g][#b]?)?\\s*)([|-].*)$")
    }
}
