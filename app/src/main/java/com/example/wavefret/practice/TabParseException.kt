package com.example.wavefret.practice

/**
 * Raised when tab text cannot be turned into an [Exercise].
 *
 * @property lineNumber One-based line where the problem was found, or null
 *   when the problem is with the tab as a whole. Type: Int?
 * @param problemDescription What was wrong. Type: String
 */
class TabParseException(
    val lineNumber: Int?,
    problemDescription: String
) : IllegalArgumentException(
    if (lineNumber == null) problemDescription else "Line $lineNumber: $problemDescription"
)
