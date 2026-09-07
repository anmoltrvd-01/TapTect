package com.example.taptect.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun WaveformVisualizer(
    audioData: ShortArray,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF6200EE)
) {
    // Calculate RMS for a simple intensity/amplitude scaling
    val maxAmplitude = 32767f
    val rms = if (audioData.isNotEmpty()) {
        val sum = audioData.fold(0.0) { acc, s -> acc + (s.toInt() * s.toInt()) }
        kotlin.math.sqrt(sum / audioData.size).toFloat()
    } else 0f

    // Animate the intensity for smooth visual transitions
    val animatedIntensity by animateFloatAsState(
        targetValue = (rms / maxAmplitude).coerceIn(0.05f, 1f),
        animationSpec = tween(durationMillis = 100),
        label = "WaveformIntensity"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        val path = Path()
        path.moveTo(0f, centerY)

        if (audioData.isNotEmpty()) {
            // Downsample for smoother visualization
            val step = (audioData.size / 50).coerceAtLeast(1)
            val points = mutableListOf<Pair<Float, Float>>()
            
            for (i in 0 until audioData.size step step) {
                val x = (i.toFloat() / audioData.size) * width
                // Normalize and scale by animated intensity
                val y = centerY + (audioData[i].toFloat() / maxAmplitude) * centerY * animatedIntensity * 2f
                points.add(x to y)
            }

            // Draw smoothed curve using quadratic bezier
            for (i in 0 until points.size - 1) {
                val p1 = points[i]
                val p2 = points[i + 1]
                val midX = (p1.first + p2.first) / 2f
                val midY = (p1.second + p2.second) / 2f
                
                if (i == 0) {
                    path.lineTo(p1.first, p1.second)
                } else {
                    path.quadraticTo(p1.first, p1.second, midX, midY)
                }
            }
            
            // Close to the end
            if (points.isNotEmpty()) {
                path.lineTo(width, centerY)
            }
        } else {
            path.lineTo(width, centerY)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
