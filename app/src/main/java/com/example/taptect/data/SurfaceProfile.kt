package com.example.taptect.data

/**
 * Defines the acoustic and physical characteristics of a surface material.
 */
data class SurfaceProfile(
    val materialName: String,
    val targetFrequencyRange: IntRange,
    val decayRateThreshold: Float,
    val densityScore: Int // 0-100
)

data class SurfaceResult(
    val material: SurfaceProfile,
    val isHollow: Boolean,
    val confidence: Float
)
