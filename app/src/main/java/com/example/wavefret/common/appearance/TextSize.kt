package com.example.wavefret.common.appearance

/**
 * The player's in-app text size, applied on top of the phone's own font
 * size so accessibility settings are never undone.
 *
 * @property storageKey Stable name written to preferences. Type: String
 * @property scaleMultiplier Factor applied to the system font scale. Type: Float
 */
enum class TextSize(val storageKey: String, val scaleMultiplier: Float) {
    /** Smaller than the phone's text. */
    SMALL("small", 0.85f),

    /** Same as the phone's text. */
    DEFAULT("default", 1.0f),

    /** Larger than the phone's text. */
    LARGE("large", 1.15f),

    /** Much larger than the phone's text. */
    EXTRA_LARGE("extra_large", 1.3f);

    /**
     * @param systemFontScale The phone's font scale. Type: Float
     * @return The font scale to use in the app, at most [MAXIMUM_FONT_SCALE]. Type: Float
     */
    fun fontScaleFor(systemFontScale: Float): Float {
        return (systemFontScale * scaleMultiplier).coerceAtMost(MAXIMUM_FONT_SCALE)
    }

    companion object {
        /** Largest font scale Android supports. Type: Float */
        const val MAXIMUM_FONT_SCALE = 2.0f

        /**
         * @param storageKey Value read from preferences, or null if never set. Type: String?
         * @return The matching size, or [DEFAULT] when missing or unknown. Type: TextSize
         */
        fun fromStorageKey(storageKey: String?): TextSize {
            return entries.firstOrNull { it.storageKey == storageKey } ?: DEFAULT
        }
    }
}
