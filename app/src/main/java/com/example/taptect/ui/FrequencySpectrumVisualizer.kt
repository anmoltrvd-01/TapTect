package com.example.taptect.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

        // Only show the audible range
        val displayCount = (magnitudes.size / 4).coerceAtLeast(10)
        val barWidth = width / displayCount
        val spacing = 2.dp.toPx()

        for (i in 0 until displayCount) {
            val magnitude = magnitudes[i]
            val normalizedMag = (magnitude / 1000f).coerceIn(0.01f, 1f)
            val barHeight = normalizedMag * height

            drawRoundRect(
                color = color,
                topLeft = Offset(i * barWidth + spacing / 2, height - barHeight),
                size = Size((barWidth - spacing).coerceAtLeast(1f), barHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
}
