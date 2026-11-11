package com.example.wavefret.practice.progression

/**
 * The outcome of one completed run through an exercise; together these
 * form the player's personal hit/miss history.
 *
 * @property exerciseTitle Title of the exercise played. Type: String
 * @property practiceMode Whether it was played in wait or tempo mode. Type: PracticeMode
 * @property scorePercent Notes hit as a percentage of all notes. Type: Int
 * @property hitCount Notes played correctly. Type: Int
 * @property missCount Notes missed. Type: Int
 * @property completedAtMillis Wall-clock time the run finished. Type: Long
 */
data class PracticeResult(
    val exerciseTitle: String,
    val practiceMode: PracticeMode,
    val scorePercent: Int,
    val hitCount: Int,
    val missCount: Int,
    val completedAtMillis: Long
)
