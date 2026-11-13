package com.example.wavefret.common.ui

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import com.example.wavefret.common.appearance.AppearancePreferencesStore
import com.example.wavefret.common.appearance.TextSize

/**
 * Base screen that applies the player's in-app text size on top of the
 * phone's font size, and redraws itself when the size was changed in
 * Settings while it was in the background.
 *
 * Android framework glue; the scale rule lives in the tested [TextSize].
 */
open class WaveFretActivity : AppCompatActivity() {

    private var appliedTextSize: TextSize = TextSize.DEFAULT

    /**
     * @param newBase Context the activity is attached to. Type: Context
     * @return Unit
     */
    override fun attachBaseContext(newBase: Context) {
        appliedTextSize = AppearancePreferencesStore(newBase).textSize
        val configuration = Configuration(newBase.resources.configuration)
        configuration.fontScale = appliedTextSize.fontScaleFor(systemFontScaleOf(newBase))
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    /**
     * @return Unit
     */
    override fun onResume() {
        super.onResume()
        if (AppearancePreferencesStore(this).textSize != appliedTextSize) recreate()
    }

    /**
     * @param context Context whose application holds the phone's own configuration. Type: Context
     * @return The phone's font scale, unaffected by WaveFret's text size. Type: Float
     */
    private fun systemFontScaleOf(context: Context): Float {
        return context.applicationContext?.resources?.configuration?.fontScale
            ?: context.resources.configuration.fontScale
    }
}
