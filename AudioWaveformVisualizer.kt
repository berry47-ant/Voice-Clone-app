package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun AudioWaveformVisualizer(
    isRecording: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 28,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    idleColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        val totalWidth = size.width
        val canvasHeight = size.height
        val barWidth = (totalWidth / (barCount * 1.5f)).coerceAtLeast(3.dp.toPx())
        val spacing = (totalWidth - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)

        val centerY = canvasHeight / 2f
        val maxBarHeight = canvasHeight * 0.9f

        for (i in 0 until barCount) {
            val fraction = i.toFloat() / barCount.toFloat()
            val sineWave = (sin(phase + fraction * 4.5f) + 1f) / 2f

            val heightMultiplier = if (isRecording) {
                // Combine sine wave variation with real microphone amplitude
                val ampFactor = (amplitude * 1.8f).coerceIn(0.15f, 1f)
                (0.2f + sineWave * 0.4f + ampFactor * 0.6f).coerceIn(0.1f, 1f)
            } else {
                // Static decorative wave pattern
                (0.18f + sin(fraction * 3.14f) * 0.35f).coerceIn(0.12f, 0.6f)
            }

            val barHeight = (maxBarHeight * heightMultiplier).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + spacing)
            val y = centerY - (barHeight / 2f)

            val barColor = if (isRecording) activeColor else idleColor

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
