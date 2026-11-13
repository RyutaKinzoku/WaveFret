package com.example.wavefret.common.appearance

/**
 * The player's choice of colour theme, chosen inside the app.
 *
 * @property storageKey Stable name written to preferences. Type: String
 */
enum class ThemeMode(val storageKey: String) {
    /** Follow the phone's light or dark setting. */
    SYSTEM("system"),

    /** Always light. */
    LIGHT("light"),

    /** Always dark. */
    DARK("dark");

    companion object {
        /**
         * @param storageKey Value read from preferences, or null if never set. Type: String?
         * @return The matching mode, or [SYSTEM] when missing or unknown. Type: ThemeMode
         */
        fun fromStorageKey(storageKey: String?): ThemeMode {
            return entries.firstOrNull { it.storageKey == storageKey } ?: SYSTEM
        }
    }
}
