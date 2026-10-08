package com.haptix.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.haptix.app.ui.theme.HaptiXShapeTokens
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.LocalReducedMotion
import kotlin.math.PI
import kotlin.math.sin

/**
 * Reusable Multimodal Haptic Waveform Visualizer.
 *
 * Renders synchronized visual frequency traces, background coordinate grid,
 * signal particles, cyan primary sensory trace, and violet multimodal trace.
 */
@Composable
fun HaptiXWaveform(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(180.dp),
    frequencyHz: Float = 220f,
    amplitude: Float = 0.8f
) {
    val reducedMotion = LocalReducedMotion.current
    val infiniteTransition = rememberInfiniteTransition(label = "waveformAnimation")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val colors = HaptiXThemeTokens.colors
    val cyanColor = colors.accentPrimary
    val violetColor = colors.accentSecondary
    val gridColor = colors.borderSubtle
    val bgColor = colors.surface

    Box(
        modifier = modifier
            .clip(HaptiXShapeTokens.hero)
            .background(bgColor)
            .border(1.dp, colors.borderSubtle, HaptiXShapeTokens.hero)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h * 0.52f

            // 1. Subtle Coordinate Grid Lines
            val gridStepX = w / 8f
            for (i in 1..7) {
                drawLine(
                    color = gridColor.copy(alpha = 0.4f),
                    start = Offset(gridStepX * i, 0f),
                    end = Offset(gridStepX * i, h),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
            val gridStepY = h / 4f
            for (i in 1..3) {
                drawLine(
                    color = gridColor.copy(alpha = 0.4f),
                    start = Offset(0f, gridStepY * i),
                    end = Offset(w, gridStepY * i),
                    strokeWidth = 0.5.dp.toPx()
                )
            }

            // 2. Violet Multimodal Secondary Waveform (Background depth)
            val violetPath = Path()
            val violetCycles = 2.4f
            val violetAmp = h * 0.22f * amplitude
            for (x in 0..w.toInt() step 4) {
                val relX = x / w
                val y = midY + sin((relX * violetCycles * 2 * PI + phase * 0.7f).toFloat()) * violetAmp
                if (x == 0) violetPath.moveTo(0f, y) else violetPath.lineTo(x.toFloat(), y)
            }
            drawPath(
                path = violetPath,
                color = violetColor.copy(alpha = 0.55f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Cyan Primary Tactile Frequency Waveform
            val cyanPath = Path()
            val cyanCycles = 3.6f
            val cyanAmp = h * 0.28f * amplitude
            for (x in 0..w.toInt() step 3) {
                val relX = x / w
                val envelope = sin(relX * PI).toFloat() // Dampened at edges
                val y = midY + sin((relX * cyanCycles * 2 * PI - phase).toFloat()) * cyanAmp * envelope
                if (x == 0) cyanPath.moveTo(0f, y) else cyanPath.lineTo(x.toFloat(), y)
            }

            // Glow trace
            drawPath(
                path = cyanPath,
                color = cyanColor.copy(alpha = 0.25f),
                style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
            )
            // Core illuminated signal
            drawPath(
                path = cyanPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        cyanColor.copy(alpha = 0.4f),
                        cyanColor,
                        cyanColor.copy(alpha = 0.8f)
                    )
                ),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // 4. Signal Particles / Nodes along key peaks
            val numParticles = 5
            for (i in 1..numParticles) {
                val partRelX = (i.toFloat() / (numParticles + 1))
                val env = sin(partRelX * PI).toFloat()
                val partY = midY + sin((partRelX * cyanCycles * 2 * PI - phase).toFloat()) * cyanAmp * env
                drawCircle(
                    color = cyanColor,
                    radius = 3.dp.toPx(),
                    center = Offset(partRelX * w, partY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.2.dp.toPx(),
                    center = Offset(partRelX * w, partY)
                )
            }
        }
    }
}
