package com.example.wavefret.practice

import com.example.wavefret.common.time.BeatClock

/**
 * Tempo mode: one note per beat after a count-in. Each note can be hit
 * within half a beat either side of its beat; once that window closes the
 * note is scored as a miss and the next one becomes current.
 *
 * Pitch detection needs ~50-90 ms windows on low notes, so this judges
 * "on the beat" at beat-window resolution, not millisecond timing.
 *
 * @property exercise The exercise being practiced. Type: Exercise
 * @param noteMatcher Decides whether a pitch is the expected note. Type: NoteMatcher
 * @param hitConfirmer Turns matching readings into discrete hits. Type: HitConfirmer
 * @property countInBeats Metronome beats before the first note is due. Type: Int
 */
class TempoModePracticeSession(
    override val exercise: Exercise,
    noteMatcher: NoteMatcher,
    hitConfirmer: HitConfirmer,
    val countInBeats: Int = DEFAULT_COUNT_IN_BEATS
) : PracticeSession {

    private val expectedNoteJudge = ExpectedNoteJudge(exercise, noteMatcher, hitConfirmer)
    private var beatClock: BeatClock? = null
    private var currentNoteIndex = 0
    private var hitCount = 0
    private var missCount = 0
    private var lastReading = PitchReading.SILENT
    private var playheadPosition = -countInBeats.toDouble()

    /** True once [start] has been called since creation or the last restart. Type: Boolean */
    val isStarted: Boolean get() = beatClock != null

    /**
     * Starts the count-in; the first note is due countInBeats beats later.
     *
     * @param startTimeMillis Monotonic time of the first count-in beat. Type: Long
     * @return The clock the metronome should follow. Type: BeatClock
     */
    fun start(startTimeMillis: Long): BeatClock {
        return BeatClock(exercise.tempoBpm, startTimeMillis).also { beatClock = it }
    }

    /**
     * @param frequencyHz Pitch detected in the latest buffer, or null for none. Type: Double?
     * @param timestampMillis Monotonic time the buffer was captured at. Type: Long
     * @return Progress after taking this reading into account. Type: PracticeProgress
     */
    override fun onPitchDetected(frequencyHz: Double?, timestampMillis: Long): PracticeProgress {
        val activeBeatClock = beatClock ?: return currentProgress()
        playheadPosition = activeBeatClock.beatPositionAt(timestampMillis) - countInBeats
        missNotesWhoseWindowClosed()
        if (currentNoteIndex >= exercise.noteCount) return currentProgress()

        lastReading = expectedNoteJudge.classify(currentNoteIndex, frequencyHz)
        val isInsideHitWindow = playheadPosition >= currentNoteIndex - HIT_WINDOW_HALF_WIDTH_BEATS
        if (isInsideHitWindow && expectedNoteJudge.confirmsHit(lastReading)) {
            hitCount++
            advancePastCurrentNote()
        }
        return currentProgress()
    }

    /** @return Progress without consuming a new reading. Type: PracticeProgress */
    override fun currentProgress(): PracticeProgress = PracticeProgress(
        currentNoteIndex = currentNoteIndex,
        noteCount = exercise.noteCount,
        hitCount = hitCount,
        missCount = missCount,
        lastReading = lastReading,
        playheadPosition = playheadPosition
    )

    /** @return Progress after going back to the start; [start] must be called again. Type: PracticeProgress */
    override fun restart(): PracticeProgress {
        beatClock = null
        currentNoteIndex = 0
        hitCount = 0
        missCount = 0
        lastReading = PitchReading.SILENT
        playheadPosition = -countInBeats.toDouble()
        expectedNoteJudge.reset()
        return currentProgress()
    }

    /**
     * Scores as missed every note whose hit window the playhead has passed.
     *
     * @return Unit
     */
    private fun missNotesWhoseWindowClosed() {
        while (currentNoteIndex < exercise.noteCount &&
            playheadPosition > currentNoteIndex + HIT_WINDOW_HALF_WIDTH_BEATS
        ) {
            missCount++
            advancePastCurrentNote()
        }
    }

    /**
     * @return Unit
     */
    private fun advancePastCurrentNote() {
        expectedNoteJudge.onAdvancedPast(currentNoteIndex)
        currentNoteIndex++
    }

    private companion object {
        const val DEFAULT_COUNT_IN_BEATS = 4
        const val HIT_WINDOW_HALF_WIDTH_BEATS = 0.5
    }
}
