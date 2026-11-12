package com.example.wavefret.common.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Turns a stream of small capture buffers into overlapping analysis
 * windows: a full window every hop. Low notes need long windows (low E
 * repeats only every 24 ms), but readings should still arrive often, so a
 * long window slides forward in short hops.
 *
 * @param innerSource Where the small capture buffers come from. Type: PcmAudioSource
 * @param windowSamples Length of each emitted window. Type: Int
 * @param hopSamples How far each window moves on from the previous one. Type: Int
 */
class SlidingWindowAudioSource(
    private val innerSource: PcmAudioSource,
    private val windowSamples: Int,
    private val hopSamples: Int
) : PcmAudioSource {

    init {
        require(windowSamples > 0 && hopSamples in 1..windowSamples) { "Hop must be between 1 and the window length" }
    }

    /** Same rate as the inner source. Type: Int */
    override val sampleRateHz: Int get() = innerSource.sampleRateHz

    /**
     * @return Overlapping windows of [windowSamples], one every [hopSamples]. Type: Flow<FloatArray>
     */
    override fun audioBufferStream(): Flow<FloatArray> = flow {
        var pending = FloatArray(windowSamples * 2)
        var pendingCount = 0
        innerSource.audioBufferStream().collect { captureBuffer ->
            if (pendingCount + captureBuffer.size > pending.size) {
                pending = pending.copyOf(maxOf(pending.size * 2, pendingCount + captureBuffer.size))
            }
            captureBuffer.copyInto(pending, pendingCount)
            pendingCount += captureBuffer.size
            while (pendingCount >= windowSamples) {
                emit(pending.copyOf(windowSamples))
                pending.copyInto(pending, destinationOffset = 0, startIndex = hopSamples, endIndex = pendingCount)
                pendingCount -= hopSamples
            }
        }
    }
}
