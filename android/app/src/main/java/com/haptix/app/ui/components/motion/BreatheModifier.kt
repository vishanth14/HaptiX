package com.haptix.app.ui.components.motion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import com.haptix.app.ui.theme.LocalReducedMotion

/**
 * Modifier that applies a slow, organic breathing effect (micro-scale and subtle alpha)
 * for active research states (e.g. active tactile stimulus, haptics active).
 *
 * Cycle: 2.8s gentle oscillation between 1.0f and 1.015f.
 * Suppressed if [LocalReducedMotion] is true.
 */
@Composable
fun Modifier.breathe(
    enabled: Boolean = true,
    minScale: Float = 1.0f,
    maxScale: Float = 1.015f,
    minAlpha: Float = 0.88f,
    maxAlpha: Float = 1.0f,
    durationMs: Int = 2800
): Modifier {
    if (!enabled) return this

    val reducedMotion = LocalReducedMotion.current
    if (reducedMotion) return this

    val infiniteTransition = rememberInfiniteTransition(label = "breatheTransition")

    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheScale"
    )

    val alphaVal by infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breatheAlpha"
    )

    return this
        .scale(scale)
        .alpha(alphaVal)
}
