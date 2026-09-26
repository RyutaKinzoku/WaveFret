package com.example.wavefret.tuner

/**
 * A named set of strings a bass can be tuned to. Adding a new tuning (a
 * 5-string set, Drop D, etc.) means adding a new BassTuning value — no
 * change to TuningEvaluator or anything else that consumes one.
 *
 * @property name Display name for the tuning (e.g. "Standard", "Drop D"). Type: String
 * @property strings The strings that make up this tuning, low to high. Type: List<BassString>
 */
data class BassTuning(
    val name: String,
    val strings: List<BassString>
) {
    init {
        require(strings.isNotEmpty()) { "BassTuning must contain at least one string" }
    }
}