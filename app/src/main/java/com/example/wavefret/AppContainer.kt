package com.example.wavefret

import android.content.Context
import com.example.wavefret.common.permission.MicrophonePermissionManager
import com.example.wavefret.common.permission.SystemPermissionChecker
import com.example.wavefret.common.storage.AppStorageDirectoryProvider
import com.example.wavefret.common.storage.ExternalAppStorageDirectoryProvider
import com.example.wavefret.common.time.Clock
import com.example.wavefret.common.time.DateFormatter
import com.example.wavefret.common.time.DurationFormatter
import com.example.wavefret.common.time.SystemClock
import com.example.wavefret.common.time.SystemDateFormatter

/**
 * Composition root: builds the app's shared, feature-independent
 * dependencies exactly once, so Activities depend on this container
 * instead of each constructing common infrastructure themselves.
 *
 * @property context Application context used to build Android-backed dependencies. Type: Context
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
}