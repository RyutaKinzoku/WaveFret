package com.example.wavefret.screens

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.wavefret.MainActivity
import com.example.wavefret.R
import com.example.wavefret.practice.PracticeActivity
import com.example.wavefret.practice.SongLibraryActivity
import com.example.wavefret.recording.RecorderActivity
import com.example.wavefret.tuner.TunerActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Launches every screen under Robolectric with real (native) graphics: each
 * must inflate with the app theme without crashing, show its primary action,
 * and is rendered to `app/build/screenshots/` in light and dark for review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenRenderTest {

    @Test
    fun homeShowsThePracticeActionInLightTheme() = renderAndCheck(MainActivity::class.java, "home_light", R.id.homePracticeButton)

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun homeShowsThePracticeActionInDarkTheme() = renderAndCheck(MainActivity::class.java, "home_dark", R.id.homePracticeButton)

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun recorderShowsTheRecordButton() = renderAndCheck(RecorderActivity::class.java, "recorder_dark", R.id.recorderToggleButton)

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun recorderListsAudioAndVideoTakesWithShareButtons() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val storageFolder = context.getExternalFilesDir(null) ?: context.filesDir
        File(storageFolder, "recordings").apply { mkdirs() }.let { recordingsFolder ->
            File(recordingsFolder, "recording_1.m4a").writeBytes(byteArrayOf(0))
            File(recordingsFolder, "recording_2.mp4").writeBytes(byteArrayOf(0))
        }
        renderAndCheck(RecorderActivity::class.java, "recorder_takes_dark", R.id.recorderTakesRecyclerView)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun tunerShowsTheGauge() = renderAndCheck(TunerActivity::class.java, "tuner_dark", R.id.tunerGaugeView)

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun practiceShowsTheStartButton() = renderAndCheck(PracticeActivity::class.java, "practice_dark", R.id.practiceStartButton)

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun songLibraryShowsTheAddSongButton() =
        renderAndCheck(SongLibraryActivity::class.java, "library_dark", R.id.songLibraryAddSongButton)

    @Test
    fun practiceShowsTheStartButtonInLightTheme() =
        renderAndCheck(PracticeActivity::class.java, "practice_light", R.id.practiceStartButton)

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun practiceStartButtonSaysStartBeforePlaying() {
        ActivityScenario.launch(PracticeActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(
                    activity.getString(R.string.practice_start),
                    activity.findViewById<Button>(R.id.practiceStartButton).text.toString()
                )
            }
        }
    }

    /**
     * @param activityClass Screen to launch. Type: Class<T>
     * @param screenshotName File name, without extension. Type: String
     * @param primaryViewId View that must be laid out and visible. Type: Int
     * @return Unit
     */
    private fun <T : Activity> renderAndCheck(activityClass: Class<T>, screenshotName: String, primaryViewId: Int) {
        ActivityScenario.launch(activityClass).use { scenario ->
            scenario.onActivity { activity ->
                val primaryView = activity.findViewById<View>(primaryViewId)
                assertTrue("primary view must be shown", primaryView.isShown && primaryView.height > 0)
                saveScreenshot(activity.window.decorView, screenshotName)
            }
        }
    }

    /**
     * @param rootView View to render. Type: View
     * @param screenshotName File name, without extension. Type: String
     * @return Unit
     */
    private fun saveScreenshot(rootView: View, screenshotName: String) {
        val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        rootView.draw(Canvas(bitmap))
        val screenshotFolder = File("build/screenshots").apply { mkdirs() }
        File(screenshotFolder, "$screenshotName.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
