package com.example.taptect.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
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
    val maxAmplitude = 32767f
    
    // Calculate RMS only when audioData changes
    val rms = remember(audioData) {
        if (audioData.isNotEmpty()) {
            var sum = 0.0
            for (s in audioData) {
                sum += (s.toInt() * s.toInt()).toDouble()
            }
            kotlin.math.sqrt(sum / audioData.size).toFloat()
        } else 0f
    }

    val animatedIntensity by animateFloatAsState(
        targetValue = (rms / maxAmplitude).coerceIn(0.01f, 1f),
        animationSpec = tween(durationMillis = 100),
        label = "WaveformIntensity"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        val path = Path()
        path.moveTo(0f, centerY)

        if (audioData.isNotEmpty()) {
            val step = (audioData.size / 60).coerceAtLeast(1)
            val pointsX = FloatArray(60)
            val pointsY = FloatArray(60)
            var count = 0
            
            for (i in 0 until audioData.size step step) {
                if (count >= 60) break
                pointsX[count] = (i.toFloat() / audioData.size) * width
                pointsY[count] = centerY + (audioData[i].toFloat() / maxAmplitude) * centerY * animatedIntensity * 3f
                count++
            }

            for (i in 0 until count - 1) {
                val p1x = pointsX[i]
                val p1y = pointsY[i]
                val p2x = pointsX[i + 1]
                val p2y = pointsY[i + 1]
                
                val midX = (p1x + p2x) / 2f
                val midY = (p1y + p2y) / 2f
                
                if (i == 0) {
                    path.lineTo(p1x, p1y)
                } else {
                    path.quadraticTo(p1x, p1y, midX, midY)
                }
            }
            path.lineTo(width, centerY)
        } else {
            path.lineTo(width, centerY)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
