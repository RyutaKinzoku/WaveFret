package com.example.wavefret.common.appearance

import android.content.Context
import androidx.core.content.edit

/**
 * Keeps the player's theme and text size in private app preferences.
 *
 * Thin SharedPreferences wrapper, verified on-device; parsing and
 * defaults live in the tested [ThemeMode] and [TextSize].
 *
 * @param context Context used to open the preferences. Type: Context
 */
class AppearancePreferencesStore(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** The chosen theme, [ThemeMode.SYSTEM] until the player picks one. Type: ThemeMode */
    var themeMode: ThemeMode
        get() = ThemeMode.fromStorageKey(preferences.getString(THEME_MODE_KEY, null))
        set(value) = preferences.edit { putString(THEME_MODE_KEY, value.storageKey) }

    /** The chosen text size, [TextSize.DEFAULT] until the player picks one. Type: TextSize */
    var textSize: TextSize
        get() = TextSize.fromStorageKey(preferences.getString(TEXT_SIZE_KEY, null))
        set(value) = preferences.edit { putString(TEXT_SIZE_KEY, value.storageKey) }

    private companion object {
        const val PREFERENCES_NAME = "appearance"
        const val THEME_MODE_KEY = "theme_mode"
        const val TEXT_SIZE_KEY = "text_size"
    }
}
