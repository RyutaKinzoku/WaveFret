package com.example.wavefret.common.audio

/** Makes the audible metronome click, so timing logic can be tested without sound. */
interface MetronomeClickPlayer {

    /**
     * @param isAccented True for the louder first beat of a bar. Type: Boolean
     * @return Unit
     */
    fun playClick(isAccented: Boolean)
}
