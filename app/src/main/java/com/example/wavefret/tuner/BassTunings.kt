package com.example.wavefret.tuner

/** Built-in bass tunings the app ships with. */
object BassTunings {

    /** Standard 4-string tuning: E1, A1, D2, G2. Type: BassTuning */
    val STANDARD = BassTuning(
        name = "Standard",
        strings = listOf(
            BassString("E", 41.20),
            BassString("A", 55.00),
            BassString("D", 73.42),
            BassString("G", 98.00)
        )
    )
}