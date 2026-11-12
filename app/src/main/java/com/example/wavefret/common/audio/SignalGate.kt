package com.example.wavefret.common.audio

/** Decides whether an audio buffer holds something worth analysing, or just silence and background noise. */
interface SignalGate {

    /**
     * @param samples Audio buffer in -1..1. Type: FloatArray
     * @return True if the buffer should be analysed. Type: Boolean
     */
    fun hasSignal(samples: FloatArray): Boolean
}
