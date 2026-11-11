package com.example.wavefret.practice.progression

/** Which songs the library shows. */
enum class SongSource {
    /** Built-in exercises and imported songs together. */
    ALL,

    /** Only songs the player added. */
    MY_SONGS,

    /** Only exercises shipped with the app. */
    BUILT_IN
}

/** How the library orders songs. */
enum class SongSortOrder {
    EASIEST_FIRST,
    HARDEST_FIRST,
    TITLE
}

/**
 * What the song library is currently showing: a source filter, a search
 * over titles and a sort order, defaulting to everything, easiest first.
 *
 * @property searchText Text a title must contain, ignoring case and surrounding spaces. Type: String
 * @property source Which songs to include. Type: SongSource
 * @property sortOrder How to order them. Type: SongSortOrder
 */
data class SongLibraryQuery(
    val searchText: String = "",
    val source: SongSource = SongSource.ALL,
    val sortOrder: SongSortOrder = SongSortOrder.EASIEST_FIRST
) {

    /**
     * @param libraryItems Every song in the library. Type: List<ExercisePickerItem>
     * @return The songs matching this query, in its order. Type: List<ExercisePickerItem>
     */
    fun apply(libraryItems: List<ExercisePickerItem>): List<ExercisePickerItem> {
        val normalizedSearchText = searchText.trim()
        val matchingItems = libraryItems.filter { libraryItem ->
            val isFromSource = when (source) {
                SongSource.ALL -> true
                SongSource.MY_SONGS -> libraryItem.isImported
                SongSource.BUILT_IN -> !libraryItem.isImported
            }
            isFromSource && libraryItem.exercise.title.contains(normalizedSearchText, ignoreCase = true)
        }
        return when (sortOrder) {
            SongSortOrder.EASIEST_FIRST -> matchingItems.sortedBy { it.difficultyScore }
            SongSortOrder.HARDEST_FIRST -> matchingItems.sortedByDescending { it.difficultyScore }
            SongSortOrder.TITLE -> matchingItems.sortedBy { it.exercise.title.lowercase() }
        }
    }
}
