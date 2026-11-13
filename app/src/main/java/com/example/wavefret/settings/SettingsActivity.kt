package com.example.wavefret.settings

import android.os.Bundle
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import com.example.wavefret.R
import com.example.wavefret.common.appearance.AppearancePreferencesStore
import com.example.wavefret.common.appearance.TextSize
import com.example.wavefret.common.appearance.ThemeMode
import com.example.wavefret.common.appearance.applyThemeMode
import com.example.wavefret.common.ui.WaveFretActivity
import com.example.wavefret.common.ui.applySystemBarPadding
import com.google.android.material.appbar.MaterialToolbar

/**
 * Lets the player choose the theme (follow the phone, light or dark) and
 * the text size used inside WaveFret. Changes apply at once.
 *
 * Android framework glue; the choices are the tested [ThemeMode] and
 * [TextSize].
 */
class SettingsActivity : WaveFretActivity() {

    private val appearancePreferencesStore by lazy { AppearancePreferencesStore(this) }

    /**
     * @param savedInstanceState Previously saved state, or null on first creation. Type: Bundle?
     * @return Unit
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        findViewById<android.view.View>(R.id.settingsRoot).applySystemBarPadding(applyTop = true, applyBottom = true)
        val topAppBar = findViewById<MaterialToolbar>(R.id.topAppBar)
        topAppBar.setTitle(R.string.settings_title)
        topAppBar.setNavigationOnClickListener { finish() }

        bindThemeChoices(findViewById(R.id.settingsThemeRadioGroup))
        bindTextSizeChoices(findViewById(R.id.settingsTextSizeRadioGroup))
    }

    /**
     * @param themeRadioGroup Radio buttons for the theme. Type: RadioGroup
     * @return Unit
     */
    private fun bindThemeChoices(themeRadioGroup: RadioGroup) {
        val buttonIdsByThemeMode = mapOf(
            ThemeMode.SYSTEM to R.id.settingsThemeSystemRadioButton,
            ThemeMode.LIGHT to R.id.settingsThemeLightRadioButton,
            ThemeMode.DARK to R.id.settingsThemeDarkRadioButton
        )
        themeRadioGroup.check(buttonIdsByThemeMode.getValue(appearancePreferencesStore.themeMode))
        themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val chosenThemeMode = buttonIdsByThemeMode.entries.first { it.value == checkedId }.key
            if (chosenThemeMode != appearancePreferencesStore.themeMode) {
                appearancePreferencesStore.themeMode = chosenThemeMode
                applyThemeMode(chosenThemeMode)
            }
        }
    }

    /**
     * @param textSizeRadioGroup Radio buttons for the text size. Type: RadioGroup
     * @return Unit
     */
    private fun bindTextSizeChoices(textSizeRadioGroup: RadioGroup) {
        val buttonIdsByTextSize = mapOf(
            TextSize.SMALL to R.id.settingsTextSizeSmallRadioButton,
            TextSize.DEFAULT to R.id.settingsTextSizeDefaultRadioButton,
            TextSize.LARGE to R.id.settingsTextSizeLargeRadioButton,
            TextSize.EXTRA_LARGE to R.id.settingsTextSizeExtraLargeRadioButton
        )
        textSizeRadioGroup.check(buttonIdsByTextSize.getValue(appearancePreferencesStore.textSize))
        textSizeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val chosenTextSize = buttonIdsByTextSize.entries.first { it.value == checkedId }.key
            if (chosenTextSize != appearancePreferencesStore.textSize) {
                appearancePreferencesStore.textSize = chosenTextSize
                recreate()
            }
        }
    }
}
