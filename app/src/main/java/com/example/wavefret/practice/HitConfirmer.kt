package com.example.wavefret.practice

/**
 * Turns a stream of per-buffer [PitchReading]s into discrete "the note was
 * played" events. It requires several consecutive matching readings, so a
 * single stray reading doesn't count, and after a hit it stays disarmed
 * until the matching run is broken (silence or another note), so one
 * sustained note can't satisfy two identical notes in a row.
 *
 * When the next note is on the same string (or is the same note), a fresh
 * pluck can be required: then only an [onAttack] re-arms it, so neither a
 * fading tail nor fretting the still-ringing string counts as playing.
 *
 * @property requiredConsecutiveMatches Matching readings in a row needed for a hit. Type: Int
 */
class HitConfirmer(private val requiredConsecutiveMatches: Int = DEFAULT_REQUIRED_CONSECUTIVE_MATCHES) {

    init {
        require(requiredConsecutiveMatches > 0) { "At least one matching reading must be required" }
    }

    private var consecutiveMatchCount = 0
    private var isArmed = true
    private var isFreshAttackRequired = false

    /**
     * @param pitchReading How the latest buffer compares to the expected note. Type: PitchReading
     * @return True exactly when this reading completes a new hit. Type: Boolean
     */
    fun onReading(pitchReading: PitchReading): Boolean {
        if (isFreshAttackRequired) return confirmAfterFreshAttack(pitchReading)
        if (pitchReading != PitchReading.MATCHING) {
            consecutiveMatchCount = 0
            isArmed = true
            return false
        }
        if (!isArmed) return false
        consecutiveMatchCount++
        if (consecutiveMatchCount < requiredConsecutiveMatches) return false
        consecutiveMatchCount = 0
        isArmed = false
        return true
    }

    /**
     * Forgets any partial run and re-arms, e.g. when an exercise restarts.
     *
     * @return Unit
     */
    fun reset() {
        consecutiveMatchCount = 0
        isArmed = true
        isFreshAttackRequired = false
    }

    /**
     * From now on, only readings after a fresh pluck ([onAttack]) can confirm a hit.
     *
     * @return Unit
     */
    fun requireFreshAttack() {
        consecutiveMatchCount = 0
        isArmed = false
        isFreshAttackRequired = true
    }

    /**
     * A new pluck was heard: re-arms, so the readings that follow can confirm a hit.
     *
     * @return Unit
     */
    fun onAttack() {
        consecutiveMatchCount = 0
        isArmed = true
    }

    /**
     * @param pitchReading Latest reading, while a fresh pluck is required. Type: PitchReading
     * @return True when a plucked note has been confirmed. Type: Boolean
     */
    private fun confirmAfterFreshAttack(pitchReading: PitchReading): Boolean {
        if (!isArmed) return false
        if (pitchReading != PitchReading.MATCHING) {
            consecutiveMatchCount = 0
            return false
        }
        if (++consecutiveMatchCount < requiredConsecutiveMatches) return false
        consecutiveMatchCount = 0
        isArmed = false
        return true
    }

    private companion object {
        const val DEFAULT_REQUIRED_CONSECUTIVE_MATCHES = 2
    }
}
