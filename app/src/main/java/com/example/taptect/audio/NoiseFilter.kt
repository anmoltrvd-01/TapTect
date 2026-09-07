package com.example.taptect.audio

import kotlin.math.PI
import kotlin.math.tan

/**
 * Applies a band-pass filter to raw PCM audio buffers to isolate tap frequencies.
 */
class NoiseFilter(
    private val sampleRate: Int = 44100,
    private val lowCutoff: Float = 100f,
    private val highCutoff: Float = 8000f
) {
    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    private val a0: Float
    private val a1: Float
    private val a2: Float
    private val b1: Float
    private val b2: Float

    init {
        val centerFreq = (lowCutoff + highCutoff) / 2f
        val bandwidth = highCutoff - lowCutoff
        val omega = 2f * PI.toFloat() * centerFreq / sampleRate
        val alpha = tan(PI.toFloat() * bandwidth / sampleRate)

        val norm = 1f + alpha
        a0 = alpha / norm
        a1 = 0f
        a2 = -alpha / norm
        b1 = -2f * kotlin.math.cos(omega) / norm
        b2 = (1f - alpha) / norm
    }

    /**
     * Filters the given PCM data using a second-order IIR bandpass filter.
     */
    fun process(pcm: ShortArray): ShortArray {
        val output = ShortArray(pcm.size)
        for (i in pcm.indices) {
            val x0 = pcm[i].toFloat()
            val y0 = a0 * x0 + a1 * x1 + a2 * x2 - b1 * y1 - b2 * y2
            
            x2 = x1
            x1 = x0
            y2 = y1
            y1 = y0
            
            output[i] = y0.toInt().coerceIn(-32768, 32767).toShort()
        }
        return output
    }
}
