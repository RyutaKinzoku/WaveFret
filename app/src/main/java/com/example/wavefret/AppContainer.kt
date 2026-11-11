package com.example.wavefret

import android.content.Context
import com.example.wavefret.common.music.BassTunings
import com.example.wavefret.common.permission.MicrophonePermissionManager
import com.example.wavefret.common.permission.SystemPermissionChecker
import com.example.wavefret.common.storage.AppStorageDirectoryProvider
import com.example.wavefret.common.storage.ExternalAppStorageDirectoryProvider
import com.example.wavefret.common.time.Clock
import com.example.wavefret.common.time.DateFormatter
import com.example.wavefret.common.time.DurationFormatter
import com.example.wavefret.common.time.SystemClock
import com.example.wavefret.common.time.SystemDateFormatter
import com.example.wavefret.practice.BuiltInExercises
import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.TabTextParser
import com.example.wavefret.practice.TabTextWriter
import com.example.wavefret.practice.progression.DifficultyScorer
import com.example.wavefret.practice.progression.ExerciseProgression
import com.example.wavefret.practice.progression.PracticeResultsRepository
import com.example.wavefret.practice.songimport.ImportedExercisesRepository

/**
 * Composition root: builds the app's shared dependencies exactly once per
 * screen, so Activities depend on this container instead of each
 * constructing infrastructure themselves.
 *
 * @property context Context used to build Android-backed dependencies. Type: Context
 */
class AppContainer(private val context: Context) {

    /** Shared microphone permission policy, used by any feature needing RECORD_AUDIO. Type: MicrophonePermissionManager */
    val microphonePermissionManager: MicrophonePermissionManager by lazy {
        MicrophonePermissionManager(SystemPermissionChecker(context))
    }

    /** Shared clock, used anywhere a timestamp is needed. Type: Clock */
    val clock: Clock by lazy { SystemClock() }

    /** Shared date formatter, used anywhere a timestamp is displayed. Type: DateFormatter */
    val dateFormatter: DateFormatter by lazy { SystemDateFormatter() }

    /** Shared duration formatter, used anywhere a length of time is displayed. Type: DurationFormatter */
    val durationFormatter: DurationFormatter by lazy { DurationFormatter() }

    /** Shared app storage location, used by any feature that reads or writes files. Type: AppStorageDirectoryProvider */
    val appStorageDirectoryProvider: AppStorageDirectoryProvider by lazy {
        ExternalAppStorageDirectoryProvider(context)
    }

    /** Parser for WaveFret tab text in standard tuning. Type: TabTextParser */
    val tabTextParser: TabTextParser by lazy { TabTextParser(BassTunings.STANDARD) }

    /** Exercises shipped with the app. Type: BuiltInExercises */
    val builtInExercises: BuiltInExercises by lazy { BuiltInExercises(tabTextParser) }

    /** Songs the player imported. Type: ImportedExercisesRepository */
    val importedExercisesRepository: ImportedExercisesRepository by lazy {
        ImportedExercisesRepository(appStorageDirectoryProvider, TabTextWriter(), tabTextParser)
    }

    /** History of completed practice runs. Type: PracticeResultsRepository */
    val practiceResultsRepository: PracticeResultsRepository by lazy {
        PracticeResultsRepository(appStorageDirectoryProvider)
    }

    /** Difficulty ordering, mastery and recommendations. Type: ExerciseProgression */
    val exerciseProgression: ExerciseProgression by lazy { ExerciseProgression(DifficultyScorer()) }

    /** @return Built-in exercises followed by imported songs. Type: List<Exercise> */
    fun allExercises(): List<Exercise> = builtInExercises.all() + importedExercisesRepository.listExercises()
}
