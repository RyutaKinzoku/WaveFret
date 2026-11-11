package com.example.wavefret.practice.playalong

import com.example.wavefret.practice.Exercise
import com.example.wavefret.practice.ExpectedNoteJudge
import com.example.wavefret.practice.HitConfirmer
import com.example.wavefret.practice.NoteMatcher
import com.example.wavefret.practice.PitchReading
import com.example.wavefret.practice.PracticeProgress
import com.example.wavefret.practice.PracticeSession

/**
 * Play-along: the original song plays, and stops at each note of the
 * lesson until the player plays that note right, with unlimited tries;
 * then the song continues to the next note. Notes heard while the song is
 * playing are ignored, because the phone's speaker may be playing the
 * original bass line itself.
 *
 * The screen reports the song's position through [onMediaPosition] and
 * obeys [shouldMediaPlay]; pitch readings arrive through [onPitchDetected].
 *
 * @property exercise Timed lesson to play along with. Type: Exercise
 * @param noteMatcher Decides whether a pitch is the expected note. Type: NoteMatcher
 * @param hitConfirmer Turns matching readings into discrete hits. Type: HitConfirmer
 * @property pauseLeadMillis How long before a note's time the song stops. Type: Long
 */
class PlayAlongSession(
    override val exercise: Exercise,
    noteMatcher: NoteMatcher,
    hitConfirmer: HitConfirmer,
    private val pauseLeadMillis: Long = DEFAULT_PAUSE_LEAD_MILLIS
) : PracticeSession {

    private val noteOnsetsMillis: List<Long> =
        requireNotNull(exercise.noteOnsetsMillis) { "Play-along needs a lesson with note timings" }
    private val expectedNoteJudge = ExpectedNoteJudge(exercise, noteMatcher, hitConfirmer)
    private var currentNoteIndex = 0
    private var mediaPositionMillis = 0L
    private var lastReading = PitchReading.SILENT

    /** True while the song is stopped, waiting for the current note. Type: Boolean */
    var isWaitingForNote = false
        private set

    /** Whether the song should be playing right now. Type: Boolean */
    val shouldMediaPlay: Boolean get() = !isWaitingForNote

    /**
     * @param positionMillis The song's current position. Type: Long
     * @return Unit
     */
    fun onMediaPosition(positionMillis: Long) {
        mediaPositionMillis = positionMillis
        if (currentNoteIndex < exercise.noteCount && positionMillis >= noteOnsetsMillis[currentNoteIndex] - pauseLeadMillis) {
            isWaitingForNote = true
        }
    }

    /**
     * Re-syncs after the player scrubs the song: the next note becomes the
     * first one at or after the new position.
     *
     * @param positionMillis Where the song now is. Type: Long
     * @return Unit
     */
    fun onSeekedTo(positionMillis: Long) {
        currentNoteIndex = noteOnsetsMillis.indexOfFirst { it >= positionMillis + pauseLeadMillis }
            .let { if (it < 0) exercise.noteCount else it }
        isWaitingForNote = false
        lastReading = PitchReading.SILENT
        expectedNoteJudge.reset()
        onMediaPosition(positionMillis)
    }

    /**
     * @param frequencyHz Pitch heard in the latest buffer, or null. Type: Double?
     * @param timestampMillis Unused: the song position drives timing. Type: Long
     * @return Progress after this reading. Type: PracticeProgress
     */
    override fun onPitchDetected(frequencyHz: Double?, timestampMillis: Long): PracticeProgress {
        if (!isWaitingForNote || currentNoteIndex >= exercise.noteCount) return currentProgress()
        lastReading = expectedNoteJudge.classify(currentNoteIndex, frequencyHz)
        if (expectedNoteJudge.confirmsHit(lastReading)) {
            expectedNoteJudge.onAdvancedPast(currentNoteIndex)
            currentNoteIndex++
            isWaitingForNote = false
            onMediaPosition(mediaPositionMillis)
        }
        return currentProgress()
    }

    /** @return Progress without consuming a reading. Type: PracticeProgress */
    override fun currentProgress(): PracticeProgress = PracticeProgress(
        currentNoteIndex = currentNoteIndex,
        noteCount = exercise.noteCount,
        hitCount = currentNoteIndex,
        missCount = 0,
        lastReading = lastReading,
        playheadPosition = playheadPosition()
    )

    /** @return Progress after going back to the start of the song. Type: PracticeProgress */
    override fun restart(): PracticeProgress {
        onSeekedTo(0)
        return currentProgress()
    }

    /**
     * @return Where the tab staff should be scrolled: the waiting note, or
     *   between notes in proportion to the song's position. Type: Double
     */
    private fun playheadPosition(): Double {
        if (isWaitingForNote || currentNoteIndex >= exercise.noteCount) return currentNoteIndex.toDouble()
        val nextOnsetMillis = noteOnsetsMillis[currentNoteIndex]
        val previousOnsetMillis = if (currentNoteIndex == 0) 0L else noteOnsetsMillis[currentNoteIndex - 1]
        val gapMillis = nextOnsetMillis - previousOnsetMillis
        if (gapMillis <= 0) return currentNoteIndex.toDouble()
        val fractionOfGap = ((mediaPositionMillis - previousOnsetMillis).toDouble() / gapMillis).coerceIn(0.0, 1.0)
        return currentNoteIndex - 1 + fractionOfGap
    }

    private companion object {
        const val DEFAULT_PAUSE_LEAD_MILLIS = 40L
    }
}
