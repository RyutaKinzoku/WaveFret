package com.example.wavefret.tuner

import com.example.wavefret.common.music.BassTuning
import kotlin.math.abs

/**
 * What the tuner screen shows after a reading.
 *
 * @property targetStringIndex String being tuned, or null when all are tuned. Type: Int?
 * @property isAutomatic True when strings are tuned lowest first, false when one was chosen. Type: Boolean
 * @property tunedStringIndices Strings marked tuned. Type: Set<Int>
 * @property needleCents Needle position in cents, or null to rest it. Type: Double?
 * @property status Flat / in tune / sharp, or null when nothing is heard. Type: TuningStatus?
 * @property justTunedStringIndex The string confirmed in tune by this reading (play the chime), or null. Type: Int?
 * @property isAllTuned True once every string is tuned. Type: Boolean
 */
data class GuidedTunerState(
    val targetStringIndex: Int?,
    val isAutomatic: Boolean,
    val tunedStringIndices: Set<Int>,
    val needleCents: Double?,
    val status: TuningStatus?,
    val justTunedStringIndex: Int?,
    val isAllTuned: Boolean
)

/**
 * The tuner's brain: measures each reading against the string being tuned
 * (chosen by the player, or lowest untuned first), moves the needle
 * smoothly in real time, and when the string has stayed in tune for a
 * moment marks it tuned, reports it once (for the chime) and moves on,
 * ignoring the string's ringing for a moment so it isn't measured against
 * the next string.
 *
 * @property tuning Instrument tuning. Type: BassTuning
 * @property targetedTuningEvaluator Measures readings against one string. Type: TargetedTuningEvaluator
 * @property needleSmoother Smooths the needle. Type: NeedleSmoother
 * @property inTuneHoldDetector Confirms a string is in tune. Type: InTuneHoldDetector
 * @property afterTuneQuietMillis How long readings are ignored after a string is confirmed. Type: Long
 */
class GuidedTuner(
    private val tuning: BassTuning,
    private val targetedTuningEvaluator: TargetedTuningEvaluator = TargetedTuningEvaluator(),
    private val needleSmoother: NeedleSmoother = NeedleSmoother(),
    private val inTuneHoldDetector: InTuneHoldDetector = InTuneHoldDetector(),
    private val afterTuneQuietMillis: Long = DEFAULT_AFTER_TUNE_QUIET_MILLIS
) {

    private val tuningSession = TuningSession(tuning.strings.size)
    private var needleCents: Double? = null
    private var quietUntilMillis = Long.MIN_VALUE

    /**
     * @param frequencyHz Latest pitch reading, or null for silence. Type: Double?
     * @param timeMillis When it was taken. Type: Long
     * @return What to show. Type: GuidedTunerState
     */
    fun onReading(frequencyHz: Double?, timeMillis: Long): GuidedTunerState {
        val targetIndex = tuningSession.targetStringIndex ?: return currentState()
        if (timeMillis < quietUntilMillis) return currentState()
        val rawCents = frequencyHz?.let { targetedTuningEvaluator.centsFrom(tuning.strings[targetIndex], it) }
        needleCents = needleSmoother.onReading(rawCents, timeMillis)
        if (!inTuneHoldDetector.onReading(needleCents, timeMillis)) return currentState()

        tuningSession.markTuned(targetIndex)
        quietUntilMillis = timeMillis + afterTuneQuietMillis
        val tunedState = currentState().copy(justTunedStringIndex = targetIndex)
        startNewString()
        return tunedState
    }

    /**
     * @param stringIndex String the player tapped. Type: Int
     * @return Unit
     */
    fun select(stringIndex: Int) {
        tuningSession.select(stringIndex)
        startNewString()
    }

    /**
     * @param stringIndex String to mark out of tune. Type: Int
     * @return Unit
     */
    fun unmark(stringIndex: Int) {
        tuningSession.unmark(stringIndex)
        startNewString()
    }

    /**
     * @return Unit
     */
    fun resetAll() {
        tuningSession.resetAll()
        startNewString()
    }

    /** @return What to show now, without a new reading. Type: GuidedTunerState */
    fun currentState(): GuidedTunerState = GuidedTunerState(
        targetStringIndex = tuningSession.targetStringIndex,
        isAutomatic = tuningSession.isAutomatic,
        tunedStringIndices = tuningSession.tunedStringIndices,
        needleCents = needleCents,
        status = needleCents?.let(::statusFor),
        justTunedStringIndex = null,
        isAllTuned = tuningSession.isAllTuned
    )

    /**
     * @return Unit
     */
    private fun startNewString() {
        needleSmoother.reset()
        inTuneHoldDetector.reset()
        needleCents = null
    }

    /**
     * @param cents Needle position. Type: Double
     * @return Whether that is flat, in tune or sharp. Type: TuningStatus
     */
    private fun statusFor(cents: Double): TuningStatus = when {
        abs(cents) <= InTuneHoldDetector.DEFAULT_TOLERANCE_CENTS -> TuningStatus.IN_TUNE
        cents < 0 -> TuningStatus.FLAT
        else -> TuningStatus.SHARP
    }

    private companion object {
        const val DEFAULT_AFTER_TUNE_QUIET_MILLIS = 1_000L
    }
}
