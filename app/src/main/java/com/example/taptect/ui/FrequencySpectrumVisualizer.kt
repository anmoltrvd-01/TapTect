package com.example.taptect.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.log10

@Composable
fun FrequencySpectrumVisualizer(
    magnitudes: FloatArray,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF03DAC5)
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val width = size.width
        val height = size.height
        
        if (magnitudes.isEmpty()) return@Canvas

        // We only show up to ~10kHz for better visibility (index depends on N and sampleRate)
        // For a typical N=8192, 10kHz is around index 1800.
        // Let's just show the first half of the magnitudes array (up to Nyquist)
        val displayCount = magnitudes.size / 2 
        val barWidth = width / displayCount

        for (i in 0 until displayCount) {
            // Use log scale for magnitude visibility
            val magnitude = magnitudes[i]
            val normalizedMag = (20 * log10(magnitude.coerceAtLeast(1f)) / 100f).coerceIn(0f, 1f)
            val barHeight = normalizedMag * height

            drawRect(
                color = color,
                topLeft = Offset(i * barWidth, height - barHeight),
                size = Size(barWidth.coerceAtLeast(1f), barHeight)
            )
        }
    }
}
