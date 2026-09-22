package com.haptix.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.haptix.app.ui.theme.CyberStarGold
import com.haptix.app.ui.theme.StarEmpty
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Clean, accessible 1 to 5 star rating component for post-stimulus experimental questionnaires.
 * Elevated with smooth spring-scale interaction and high-contrast luminous fill.
 */
@Composable
fun StarRating(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 32.dp,
    touchTargetSize: Dp = 48.dp,
    enabled: Boolean = true,
    activeColor: Color = CyberStarGold,
    inactiveColor: Color = StarEmpty
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..maxStars).forEach { index ->
            val isSelected = index <= rating

            val starScale by animateFloatAsState(
                targetValue = if (isSelected) 1.08f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "starScale"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(touchTargetSize)
                    .clickable(
                        enabled = enabled,
                        role = Role.RadioButton,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = touchTargetSize / 2)
                    ) {
                        onRatingChanged(index)
                    }
                    .semantics {
                        this.role = Role.RadioButton
                        this.selected = isSelected
                        this.contentDescription = "Rate $index of $maxStars stars"
                    }
            ) {
                StarIcon(
                    isFilled = isSelected,
                    fillColor = activeColor,
                    strokeColor = if (isSelected) activeColor else inactiveColor,
                    size = starSize,
                    modifier = Modifier.scale(starScale)
                )
            }
        }
    }
}

/**
 * Geometric 5-pointed star icon drawn via Canvas with precision apex calculations.
 */
@Composable
fun StarIcon(
    isFilled: Boolean,
    fillColor: Color,
    strokeColor: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val width = this.size.width
        val height = this.size.height
        val centerX = width / 2f
        val centerY = height / 2f
        val outerRadius = width.coerceAtMost(height) / 2f * 0.95f
        val innerRadius = outerRadius * 0.42f

        val path = Path()
        val numPoints = 5
        val angleStep = (2 * PI / numPoints).toFloat()
        val startAngle = (-PI / 2).toFloat()

        for (i in 0 until numPoints) {
            val outerAngle = startAngle + i * angleStep
            val xOuter = centerX + outerRadius * cos(outerAngle)
            val yOuter = centerY + outerRadius * sin(outerAngle)

            if (i == 0) {
                path.moveTo(xOuter, yOuter)
            } else {
                path.lineTo(xOuter, yOuter)
            }

            val innerAngle = outerAngle + angleStep / 2f
            val xInner = centerX + innerRadius * cos(innerAngle)
            val yInner = centerY + innerRadius * sin(innerAngle)
            path.lineTo(xInner, yInner)
        }
        path.close()

        if (isFilled) {
            drawPath(path = path, color = fillColor, style = Fill)
            drawPath(path = path, color = fillColor, style = Stroke(width = 1.5.dp.toPx()))
        } else {
            drawPath(path = path, color = strokeColor, style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}
