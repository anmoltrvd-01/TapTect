package com.example.taptect.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Measures ambient noise to dynamically adjust trigger thresholds.
 */
class AmbientNoiseCalibrator(private val audioManager: AudioRecorderManager) {

    private var _ambientThreshold = 500f // Default fallback
    val ambientThreshold: Float get() = _ambientThreshold

    fun startCalibration(scope: CoroutineScope, onComplete: (Float) -> Unit) {
        scope.launch(Dispatchers.Default) {
            var sumRms = 0.0
            var count = 0
            
            val job = launch {
                audioManager.audioDataFlow.collect { buffer ->
                    val rms = calculateRMS(buffer)
                    sumRms += rms
                    count++
                }
            }

            delay(2000) // Measure for 2 seconds
            job.cancel()

            if (count > 0) {
                // Set threshold to 3x the average ambient noise floor
                val avgRms = (sumRms / count).toFloat()
                _ambientThreshold = (avgRms * 3f).coerceAtLeast(500f)
            }
            
            onComplete(_ambientThreshold)
        }
    }

    private fun calculateRMS(data: ShortArray): Float {
        if (data.isEmpty()) return 0f
        var sum = 0.0
        for (s in data) sum += (s.toInt() * s.toInt()).toDouble()
        return sqrt(sum / data.size).toFloat()
    }
}
