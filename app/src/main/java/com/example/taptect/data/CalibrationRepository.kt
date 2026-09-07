package com.example.taptect.data

import kotlin.math.abs

class CalibrationRepository {

    private val profiles = listOf(
        SurfaceProfile(
            materialName = "Solid Wood",
            targetFrequencyRange = 800..1500,
            decayRateThreshold = 0.3f,
            densityScore = 75
        ),
        SurfaceProfile(
            materialName = "Hollow Wall",
            targetFrequencyRange = 100..500,
            decayRateThreshold = 0.7f,
            densityScore = 30
        ),
        SurfaceProfile(
            materialName = "Metal / Steel",
            targetFrequencyRange = 2500..6000,
            decayRateThreshold = 0.5f,
            densityScore = 95
        ),
        SurfaceProfile(
            materialName = "Plastic / Composite",
            targetFrequencyRange = 600..1200,
            decayRateThreshold = 0.4f,
            densityScore = 50
        )
    )

    fun classifyTap(peakHz: Float, decayRate: Float): SurfaceResult {
        // Find best matching profile based on frequency proximity
        val bestMatch = profiles.minByOrNull { profile ->
            val midFreq = (profile.targetFrequencyRange.first + profile.targetFrequencyRange.last) / 2f
            abs(midFreq - peakHz)
        } ?: profiles[0]

        // Logic for hollow vs solid: high decay rate (prolonged vibration) usually implies a cavity
        val isHollow = decayRate > bestMatch.decayRateThreshold
        
        // Simple confidence score
        val confidence = if (peakHz in bestMatch.targetFrequencyRange.let { it.first.toFloat()..it.last.toFloat() }) 0.9f else 0.6f

        return SurfaceResult(bestMatch, isHollow, confidence)
    }
}
