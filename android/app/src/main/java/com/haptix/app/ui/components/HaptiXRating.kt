package com.haptix.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.haptix.app.ui.components.motion.bounceClick
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.LocalReducedMotion
import com.haptix.app.ui.theme.MotionTokens
import com.haptix.app.ui.theme.rememberUiHaptics
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Precision HaptiX Research Rating Control.
 *
 * Refined 1..5 tactile glyphs.
 * Selected: Electric Cyan & White illumination (NO giant yellow cartoon stars).
 * Spring bounce, color transitions, and tactile UI feedback on selection.
 */
@Composable
fun HaptiXRating(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxScore: Int = 5,
    glyphSize: Dp = 26.dp,
    touchTargetSize: Dp = 48.dp,
    enabled: Boolean = true
) {
    val uiHaptics = rememberUiHaptics()
    val reducedMotion = LocalReducedMotion.current
    val colors = HaptiXThemeTokens.colors

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..maxScore).forEach { index ->
            val isSelected = index <= rating
            val isTarget = index == rating

            val targetScale = if (isTarget && !reducedMotion) 1.14f else 1.0f
            val glyphScale by animateFloatAsState(
                targetValue = targetScale,
                animationSpec = MotionTokens.Interactive,
                label = "ratingScale_$index"
            )

            val animatedFill by animateColorAsState(
                targetValue = if (isSelected) colors.accentPrimary else Color.Transparent,
                animationSpec = MotionTokens.colorSpring(),
                label = "ratingFill_$index"
            )

            val animatedStroke by animateColorAsState(
                targetValue = if (isSelected) colors.accentPrimary else colors.textTertiary.copy(alpha = 0.6f),
                animationSpec = MotionTokens.colorSpring(),
                label = "ratingStroke_$index"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(touchTargetSize)
                    .bounceClick(
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = {
                            uiHaptics.selection()
                            onRatingChanged(index)
                        }
                    )
                    .semantics {
                        this.role = Role.RadioButton
                        this.selected = isSelected
                        this.contentDescription = "Rating value $index of $maxScore"
                    }
            ) {
                PrecisionGlyphCanvas(
                    isFilled = isSelected,
                    fillColor = animatedFill,
                    strokeColor = animatedStroke,
                    size = glyphSize,
                    modifier = Modifier.scale(glyphScale)
                )
            }
        }
    }
}

/**
 * Geometric precision 5-point star glyph rendered via Canvas.
 */
@Composable
private fun PrecisionGlyphCanvas(
    isFilled: Boolean,
    fillColor: Color,
    strokeColor: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val centerX = w / 2f
        val centerY = h / 2f
        val outerRadius = w.coerceAtMost(h) / 2f * 0.94f
        val innerRadius = outerRadius * 0.42f

        val path = Path()
        val numPoints = 5
        val angleStep = (2 * PI / numPoints).toFloat()
        val startAngle = (-PI / 2).toFloat()

        for (i in 0 until numPoints) {
            val outerAngle = startAngle + i * angleStep
            val outerX = centerX + outerRadius * cos(outerAngle)
            val outerY = centerY + outerRadius * sin(outerAngle)
            if (i == 0) path.moveTo(outerX, outerY) else path.lineTo(outerX, outerY)

            val innerAngle = outerAngle + angleStep / 2f
            val innerX = centerX + innerRadius * cos(innerAngle)
            val innerY = centerY + innerRadius * sin(innerAngle)
            path.lineTo(innerX, innerY)
        }
        path.close()

        if (isFilled) {
            drawPath(path = path, color = fillColor)
            // Core white highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 1.5.dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 1.2.dp.toPx())
        )
    }
}
