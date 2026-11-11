package com.example.wavefret.common.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Pads a view by the system bars and display cutout on the chosen edges,
 * on top of the padding it was laid out with. The app draws edge to edge
 * (mandatory from Android 15), so without this, content slides under the
 * status bar and buttons hide behind the navigation bar.
 *
 * Thin framework wrapper with no logic of its own, so per this project's
 * convention it is verified on-device rather than in JUnit.
 *
 * @receiver View to pad. Type: View
 * @param applyTop Whether to pad by the status bar / top cutout. Type: Boolean
 * @param applyBottom Whether to pad by the navigation bar / IME. Type: Boolean
 * @return Unit
 */
fun View.applySystemBarPadding(applyTop: Boolean, applyBottom: Boolean) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insetTypes = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or
            WindowInsetsCompat.Type.ime()
        val insets = windowInsets.getInsets(insetTypes)
        view.updatePadding(
            left = initialLeft + insets.left,
            top = initialTop + if (applyTop) insets.top else 0,
            right = initialRight + insets.right,
            bottom = initialBottom + if (applyBottom) insets.bottom else 0
        )
        windowInsets
    }
    ViewCompat.requestApplyInsets(this)
}
