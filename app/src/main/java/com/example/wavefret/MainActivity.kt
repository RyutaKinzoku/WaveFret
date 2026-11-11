package com.example.wavefret

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.wavefret.common.ui.applySystemBarPadding
import com.example.wavefret.practice.PracticeActivity
import com.example.wavefret.practice.SongLibraryActivity
import com.example.wavefret.practice.labelResourceFor
import com.example.wavefret.practice.progression.DifficultyScorer
import com.example.wavefret.practice.progression.PracticeStats
import com.example.wavefret.practice.progression.PracticeStatsCalculator
import com.example.wavefret.recording.RecorderActivity
import com.example.wavefret.tuner.TunerActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.NumberFormat
import java.util.TimeZone

/**
 * Home: shows progress at a glance (streak, runs this week, mastery), the
 * tools, and the single next exercise as the primary action in the thumb
 * zone. New players get onboarding copy; returning players get their streak.
 *
 * Android framework glue — the numbers come from the tested
 * [PracticeStatsCalculator] and [com.example.wavefret.practice.progression.ExerciseProgression]
 * — so it is verified manually on-device.
 */
class MainActivity : AppCompatActivity() {

    private val appContainer = AppContainer(this)
    private val practiceStatsCalculator by lazy { PracticeStatsCalculator(appContainer.exerciseProgression) }
    private val difficultyScorer = DifficultyScorer()

    private lateinit var greetingTextView: TextView
    private lateinit var streakValueTextView: TextView
    private lateinit var weekValueTextView: TextView
    private lateinit var masteredValueTextView: TextView
    private lateinit var masteryProgressIndicator: LinearProgressIndicator
    private lateinit var continueLabelTextView: TextView
    private lateinit var continueTitleTextView: TextView
    private lateinit var continueDetailTextView: TextView
    private lateinit var practiceButton: Button

    private var recommendedExerciseTitle: String? = null

    /**
     * @param savedInstanceState Previously saved state, or null on first creation. Type: Bundle?
     * @return Unit
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        findViewById<android.view.View>(R.id.homeScrollView).applySystemBarPadding(applyTop = true, applyBottom = false)
        findViewById<android.view.View>(R.id.homeContinueContainer).applySystemBarPadding(applyTop = false, applyBottom = true)

        greetingTextView = findViewById(R.id.homeGreetingTextView)
        streakValueTextView = findViewById(R.id.homeStreakValueTextView)
        weekValueTextView = findViewById(R.id.homeWeekValueTextView)
        masteredValueTextView = findViewById(R.id.homeMasteredValueTextView)
        masteryProgressIndicator = findViewById(R.id.homeMasteryProgressIndicator)
        continueLabelTextView = findViewById(R.id.homeContinueLabelTextView)
        continueTitleTextView = findViewById(R.id.homeContinueTitleTextView)
        continueDetailTextView = findViewById(R.id.homeContinueDetailTextView)
        practiceButton = findViewById(R.id.homePracticeButton)

        findViewById<MaterialCardView>(R.id.homeTunerCard).setOnClickListener {
            startActivity(Intent(this, TunerActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.homeRecorderCard).setOnClickListener {
            startActivity(Intent(this, RecorderActivity::class.java))
        }
        findViewById<Button>(R.id.homeAddSongButton).setOnClickListener {
            startActivity(SongLibraryActivity.intentFor(this, showAddSong = true))
        }
        findViewById<Button>(R.id.homeOpenLibraryButton).setOnClickListener {
            startActivity(SongLibraryActivity.intentFor(this, showAddSong = false))
        }
        practiceButton.setOnClickListener {
            startActivity(PracticeActivity.intentFor(this, recommendedExerciseTitle))
        }
    }

    /**
     * Refreshes progress every time the player comes back, e.g. after a run.
     *
     * @return Unit
     */
    override fun onResume() {
        super.onResume()
        val exercises = appContainer.allExercises()
        val results = appContainer.practiceResultsRepository.listResults()
        val nowMillis = appContainer.clock.currentTimeMillis()
        val practiceStats = practiceStatsCalculator.calculate(
            results, exercises, nowMillis, TimeZone.getDefault().getOffset(nowMillis)
        )
        renderProgress(practiceStats)

        val recommendedExercise = appContainer.exerciseProgression.recommendNext(exercises, results)
        recommendedExerciseTitle = recommendedExercise?.title
        if (recommendedExercise == null) {
            continueLabelTextView.setText(R.string.home_continue_label_all_mastered)
            continueTitleTextView.setText(R.string.home_continue_all_mastered_body)
            continueDetailTextView.isVisible = false
            practiceButton.setText(R.string.home_open_practice_button)
        } else {
            continueLabelTextView.setText(
                if (practiceStats.isNewPlayer) R.string.home_continue_label_new else R.string.home_continue_label
            )
            continueTitleTextView.text = recommendedExercise.title
            continueDetailTextView.isVisible = true
            continueDetailTextView.text = listOf(
                getString(labelResourceFor(difficultyScorer.score(recommendedExercise).level)),
                getString(R.string.practice_exercise_tempo, recommendedExercise.tempoBpm),
                resources.getQuantityString(
                    R.plurals.practice_exercise_notes, recommendedExercise.noteCount, recommendedExercise.noteCount
                )
            ).joinToString(DETAIL_SEPARATOR)
            practiceButton.text = getString(R.string.home_practice_button, recommendedExercise.title)
        }
    }

    /**
     * @param practiceStats Numbers to show. Type: PracticeStats
     * @return Unit
     */
    private fun renderProgress(practiceStats: PracticeStats) {
        greetingTextView.text = when {
            practiceStats.isNewPlayer -> getString(R.string.home_greeting_new)
            practiceStats.currentStreakDays > 1 -> getString(R.string.home_greeting_streak, practiceStats.currentStreakDays)
            else -> getString(R.string.home_greeting_returning)
        }
        val integerFormat = NumberFormat.getIntegerInstance()
        streakValueTextView.text = integerFormat.format(practiceStats.currentStreakDays)
        weekValueTextView.text = integerFormat.format(practiceStats.runsInLastSevenDays)
        masteredValueTextView.text = getString(
            R.string.home_stat_mastered_value, practiceStats.masteredExerciseCount, practiceStats.exerciseCount
        )
        masteryProgressIndicator.max = practiceStats.exerciseCount.coerceAtLeast(1)
        masteryProgressIndicator.setProgressCompat(practiceStats.masteredExerciseCount, true)
        masteryProgressIndicator.contentDescription = getString(
            R.string.home_mastery_progress_description, practiceStats.masteredExerciseCount, practiceStats.exerciseCount
        )
    }

    private companion object {
        const val DETAIL_SEPARATOR = " · "
    }
}
