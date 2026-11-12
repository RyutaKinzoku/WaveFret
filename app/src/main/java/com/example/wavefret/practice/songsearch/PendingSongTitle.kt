package com.example.wavefret.practice.songsearch

/**
 * The song the player just searched for, remembered for a while so the
 * video or tab they share back gets named after it.
 *
 * @property title Song name as typed. Type: String
 * @property searchedAtMillis Wall-clock time of the search. Type: Long
 */
data class PendingSongTitle(val title: String, val searchedAtMillis: Long) {

    /**
     * @param nowMillis Current wall-clock time. Type: Long
     * @return True if the search was recent enough to still apply. Type: Boolean
     */
    fun isFreshAt(nowMillis: Long): Boolean = nowMillis - searchedAtMillis in 0..FRESH_FOR_MILLIS

    private companion object {
        const val FRESH_FOR_MILLIS = 60 * 60 * 1_000L
    }
}
