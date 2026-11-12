package com.example.wavefret.practice

/**
 * Shared judging logic for every practice mode: classifies each reading
 * against the note now expected and decides when it has been played, while
 * keeping a sustained note from counting for a repeat of the same pitch.
 *
 * @property exercise The exercise being judged. Type: Exercise
 * @property noteMatcher Decides whether a pitch is the expected note. Type: NoteMatcher
 * @property hitConfirmer Turns matching readings into discrete hits. Type: HitConfirmer
 */
class ExpectedNoteJudge(
    private val exercise: Exercise,
    private val noteMatcher: NoteMatcher,
    private val hitConfirmer: HitConfirmer
) {

    /**
     * @param noteIndex Index of the note now expected. Type: Int
     * @param detectedFrequencyHz Pitch heard, or null for none. Type: Double?
     * @return How the reading compares to that note. Type: PitchReading
     */
    fun classify(noteIndex: Int, detectedFrequencyHz: Double?): PitchReading = when {
        detectedFrequencyHz == null -> PitchReading.SILENT
        noteMatcher.matches(expectedFrequencyHzOf(noteIndex), detectedFrequencyHz) -> PitchReading.MATCHING
        else -> PitchReading.NOT_MATCHING
    }

    /**
     * @param pitchReading Classified reading for the expected note. Type: PitchReading
     * @return True when this reading completes a hit. Type: Boolean
     */
    fun confirmsHit(pitchReading: PitchReading): Boolean = hitConfirmer.onReading(pitchReading)

    /**
     * Prepares for the next note after leaving one (hit or missed). When the
     * next note is on the same string or is the same note, it must be
     * plucked again: the ringing string or its fading tail doesn't count.
     *
     * @param leftNoteIndex Index of the note just left behind. Type: Int
     * @return Unit
     */
    fun onAdvancedPast(leftNoteIndex: Int) {
        val nextNoteIndex = leftNoteIndex + 1
        if (nextNoteIndex >= exercise.noteCount) return
        val isSamePitch = noteMatcher.matches(expectedFrequencyHzOf(leftNoteIndex), expectedFrequencyHzOf(nextNoteIndex))
        val isSameString = exercise.notes[leftNoteIndex].stringIndex == exercise.notes[nextNoteIndex].stringIndex
        hitConfirmer.reset()
        if (isSamePitch || isSameString) hitConfirmer.requireFreshAttack()
    }

    /**
     * A new pluck was heard.
     *
     * @return Unit
     */
    fun onAttack() = hitConfirmer.onAttack()

    /**
     * Forgets confirmation state, e.g. when an exercise restarts.
     *
     * @return Unit
     */
    fun reset() = hitConfirmer.reset()

    /**
     * @param noteIndex Index of a note in the exercise. Type: Int
     * @return The pitch the note should sound at, in Hz. Type: Double
     */
    private fun expectedFrequencyHzOf(noteIndex: Int): Double {
        return exercise.notes[noteIndex].expectedFrequencyHz(exercise.tuning)
    }
}
