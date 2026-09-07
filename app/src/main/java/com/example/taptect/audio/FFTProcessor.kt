package com.example.taptect.audio

import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Lightweight FFT processor for spectral analysis.
 */
class FFTProcessor {

    data class AnalysisResult(
        val peakFrequency: Float,
        val magnitudes: FloatArray,
        val energyDecay: Float
    )

    /**
     * Performs FFT on the given PCM data and returns spectral analysis.
     * @param pcm Data should be a power of 2 in length.
     * @param sampleRate The sampling rate of the audio.
     */
    fun analyze(pcm: ShortArray, sampleRate: Int): AnalysisResult {
        val n = nextPowerOfTwo(pcm.size)
        val real = FloatArray(n)
        val imag = FloatArray(n)

        // Fill real part with PCM data and apply Hanning window
        for (i in pcm.indices) {
            val window = 0.5f * (1f - cos(2f * PI.toFloat() * i / (pcm.size - 1)))
            real[i] = pcm[i].toFloat() * window
        }

        fft(real, imag)

        val magnitudes = FloatArray(n / 2)
        var maxMag = -1f
        var maxIndex = 0

        for (i in 0 until n / 2) {
            magnitudes[i] = sqrt(real[i] * real[i] + imag[i] * imag[i])
            if (magnitudes[i] > maxMag) {
                maxMag = magnitudes[i]
                maxIndex = i
            }
        }

        val peakFreq = maxIndex.toFloat() * sampleRate / n
        
        // Simple energy decay estimation (RMS of first half vs second half)
        val firstHalf = pcm.sliceArray(0 until pcm.size / 2)
        val secondHalf = pcm.sliceArray(pcm.size / 2 until pcm.size)
        val rms1 = calculateRMS(firstHalf)
        val rms2 = calculateRMS(secondHalf)
        val decay = if (rms1 > 0) rms2 / rms1 else 0f

        return AnalysisResult(peakFreq, magnitudes, decay)
    }

    private fun fft(real: FloatArray, imag: FloatArray) {
        val n = real.size
        if (n <= 1) return

        val bitReversedIndices = IntArray(n)
        for (i in 0 until n) {
            var j = 0
            var tempI = i
            var tempN = n
            while (tempN > 1) {
                j = (j shl 1) or (tempI and 1)
                tempI = tempI shr 1
                tempN = tempN shr 1
            }
            bitReversedIndices[i] = j
        }

        for (i in 0 until n) {
            val j = bitReversedIndices[i]
            if (i < j) {
                val tempR = real[i]; real[i] = real[j]; real[j] = tempR
                val tempI = imag[i]; imag[i] = imag[j]; imag[j] = tempI
            }
        }

        var length = 2
        while (length <= n) {
            val angle = -2f * PI.toFloat() / length
            val wLenR = cos(angle)
            val wLenI = sin(angle)
            for (i in 0 until n step length) {
                var wR = 1f
                var wI = 0f
                for (j in 0 until length / 2) {
                    val uR = real[i + j]
                    val uI = imag[i + j]
                    val vR = real[i + j + length / 2] * wR - imag[i + j + length / 2] * wI
                    val vI = real[i + j + length / 2] * wI + imag[i + j + length / 2] * wR
                    real[i + j] = uR + vR
                    imag[i + j] = uI + vI
                    real[i + j + length / 2] = uR - vR
                    imag[i + j + length / 2] = uI - vI
                    val nextWR = wR * wLenR - wI * wLenI
                    wI = wR * wLenI + wI * wLenR
                    wR = nextWR
                }
            }
            length *= 2
        }
    }

    private fun nextPowerOfTwo(n: Int): Int {
        var p = 1
        while (p < n) p = p shl 1
        return p
    }

    private fun calculateRMS(data: ShortArray): Float {
        if (data.isEmpty()) return 0f
        var sum = 0.0
        for (s in data) sum += s.toInt() * s.toInt()
        return sqrt(sum / data.size).toFloat()
    }
}
