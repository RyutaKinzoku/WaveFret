package com.example.wavefret.practice.songsearch

import android.content.Context
import androidx.core.content.edit

/**
 * Keeps the last searched song name in private app preferences so it
 * survives the trip to YouTube or the browser and back.
 *
 * Thin SharedPreferences wrapper, verified on-device; the freshness rule
 * lives in the tested [PendingSongTitle].
 *
 * @param context Context used to open the preferences. Type: Context
 */
class PendingSongTitleStore(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /**
     * @param pendingSongTitle Song just searched for. Type: PendingSongTitle
     * @return Unit
     */
    fun save(pendingSongTitle: PendingSongTitle) {
        preferences.edit {
            putString(TITLE_KEY, pendingSongTitle.title)
            putLong(SEARCHED_AT_KEY, pendingSongTitle.searchedAtMillis)
        }
    }

    /**
     * Returns the searched song name if it is still fresh, and forgets it.
     *
     * @param nowMillis Current wall-clock time. Type: Long
     * @return The song name, or null. Type: String?
     */
    fun takeFreshTitle(nowMillis: Long): String? {
        val title = preferences.getString(TITLE_KEY, null) ?: return null
        val pendingSongTitle = PendingSongTitle(title, preferences.getLong(SEARCHED_AT_KEY, 0L))
        preferences.edit {
            remove(TITLE_KEY)
            remove(SEARCHED_AT_KEY)
        }
        return pendingSongTitle.title.takeIf { pendingSongTitle.isFreshAt(nowMillis) }
    }

    private companion object {
        const val PREFERENCES_NAME = "song_search"
        const val TITLE_KEY = "pending_title"
        const val SEARCHED_AT_KEY = "searched_at"
    }
}
