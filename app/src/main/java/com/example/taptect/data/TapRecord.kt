package com.example.taptect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tap_records")
data class TapRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val materialType: String,
    val peakFrequency: Float,
    val decayRate: Float,
    val densityScore: Int,
    val userNote: String? = null
)
