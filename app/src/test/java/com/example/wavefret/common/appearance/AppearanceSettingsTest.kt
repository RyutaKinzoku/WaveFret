package com.example.wavefret.common.appearance

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceSettingsTest {

    @Test
    fun themeModeIsReadBackFromItsStorageKey() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromStorageKey(it.storageKey)) }
    }

    @Test
    fun missingOrUnknownThemeModeFollowsTheSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageKey(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorageKey("purple"))
    }

    @Test
    fun textSizeIsReadBackFromItsStorageKey() {
        TextSize.entries.forEach { assertEquals(it, TextSize.fromStorageKey(it.storageKey)) }
    }

    @Test
    fun missingOrUnknownTextSizeIsTheDefault() {
        assertEquals(TextSize.DEFAULT, TextSize.fromStorageKey(null))
        assertEquals(TextSize.DEFAULT, TextSize.fromStorageKey("huge"))
    }

    @Test
    fun defaultTextSizeKeepsTheSystemFontScale() {
        assertEquals(1.3f, TextSize.DEFAULT.fontScaleFor(systemFontScale = 1.3f), 0.0001f)
    }

    @Test
    fun textSizeScalesOnTopOfTheSystemFontScale() {
        assertEquals(1.15f, TextSize.LARGE.fontScaleFor(systemFontScale = 1.0f), 0.0001f)
        assertEquals(0.85f, TextSize.SMALL.fontScaleFor(systemFontScale = 1.0f), 0.0001f)
        assertEquals(1.38f, TextSize.LARGE.fontScaleFor(systemFontScale = 1.2f), 0.0001f)
    }

    @Test
    fun fontScaleNeverExceedsTwice() {
        assertEquals(2.0f, TextSize.EXTRA_LARGE.fontScaleFor(systemFontScale = 2.0f), 0.0001f)
    }

    @Test
    fun textSizesGrowInOrder() {
        val multipliers = TextSize.entries.map { it.scaleMultiplier }

        assertEquals(multipliers.sorted(), multipliers)
    }
}
