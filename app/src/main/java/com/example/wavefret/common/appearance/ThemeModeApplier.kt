package com.example.wavefret.common.appearance

import androidx.appcompat.app.AppCompatDelegate

/**
 * Applies a [ThemeMode] to every screen of the app. AppCompat recreates
 * open screens itself when the mode changes.
 *
 * @param themeMode Theme to apply. Type: ThemeMode
 * @return Unit
 */
fun applyThemeMode(themeMode: ThemeMode) {
    AppCompatDelegate.setDefaultNightMode(
        when (themeMode) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
    )
}
