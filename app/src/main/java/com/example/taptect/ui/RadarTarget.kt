package com.example.taptect.ui

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun RadarTarget(
    isImpacted: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFF00E5FF)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val impactScale = remember { androidx.compose.animation.core.Animatable(1f) }

    LaunchedEffect(isImpacted) {
        if (isImpacted) {
            impactScale.animateTo(
                targetValue = 1.8f,
                animationSpec = tween(100, easing = FastOutSlowInEasing)
            )
            impactScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        }
    }

    Canvas(modifier = modifier.size(200.dp)) {
        val center = size.center
        val baseRadius = size.width / 4f
        
        // Pulse ring
        drawCircle(
            color = activeColor.copy(alpha = 0.3f),
            radius = baseRadius * pulseScale,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Impact ring
        drawCircle(
            color = if (isImpacted) Color.White else activeColor,
            radius = baseRadius * impactScale.value,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )

        // Reticle lines
        val lineLength = 20.dp.toPx()
        drawLine(
            color = activeColor,
            start = center.copy(y = center.y - baseRadius - 10.dp.toPx()),
            end = center.copy(y = center.y - baseRadius - 10.dp.toPx() - lineLength),
            strokeWidth = 3.dp.toPx()
        )
        drawLine(
            color = activeColor,
            start = center.copy(y = center.y + baseRadius + 10.dp.toPx()),
            end = center.copy(y = center.y + baseRadius + 10.dp.toPx() + lineLength),
            strokeWidth = 3.dp.toPx()
        )
        drawLine(
            color = activeColor,
            start = center.copy(x = center.x - baseRadius - 10.dp.toPx()),
            end = center.copy(x = center.x - baseRadius - 10.dp.toPx() - lineLength),
            strokeWidth = 3.dp.toPx()
        )
        drawLine(
            color = activeColor,
            start = center.copy(x = center.x + baseRadius + 10.dp.toPx()),
            end = center.copy(x = center.x + baseRadius + 10.dp.toPx() + lineLength),
            strokeWidth = 3.dp.toPx()
        )
    }
}
