package com.example.wavefret

import android.app.Application
import com.example.wavefret.common.appearance.AppearancePreferencesStore
import com.example.wavefret.common.appearance.applyThemeMode

/**
 * Applies the player's chosen theme before the first screen is drawn.
 *
 * Android framework glue, verified on-device.
 */
class WaveFretApplication : Application() {

    /**
     * @return Unit
     */
    override fun onCreate() {
        super.onCreate()
        applyThemeMode(AppearancePreferencesStore(this).themeMode)
    }
}
