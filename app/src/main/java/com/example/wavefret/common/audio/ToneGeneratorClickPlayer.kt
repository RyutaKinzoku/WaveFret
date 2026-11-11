package com.example.wavefret.common.audio

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * [MetronomeClickPlayer] backed by [ToneGenerator] short beeps: a higher
 * tone for accented beats. Thin framework wrapper, verified on-device.
 */
class ToneGeneratorClickPlayer : MetronomeClickPlayer {

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, CLICK_VOLUME_PERCENT)

    /**
     * @param isAccented True for the louder first beat of a bar. Type: Boolean
     * @return Unit
     */
    override fun playClick(isAccented: Boolean) {
        val tone = if (isAccented) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP
        toneGenerator.startTone(tone, CLICK_DURATION_MILLIS)
    }

    /**
     * Frees the native tone generator; the player must not be used afterwards.
     *
     * @return Unit
     */
    fun release() = toneGenerator.release()

    private companion object {
        const val CLICK_VOLUME_PERCENT = 80
        const val CLICK_DURATION_MILLIS = 40
    }
}
