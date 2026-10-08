package com.haptix.app.ui.components.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import com.haptix.app.ui.theme.LocalReducedMotion
import com.haptix.app.ui.theme.MotionTokens
import com.haptix.app.ui.theme.rememberUiHaptics

/**
 * Modifier applying Apple-style tactile spring bounce response to interactive elements.
 * On tap: normal (1.0) -> compressed (0.96) -> spring overshoot (1.015) -> settle (1.0).
 * Respects [LocalReducedMotion] to suppress physics scaling when requested.
 */
@Composable
fun Modifier.bounceClick(
    enabled: Boolean = true,
    hapticFeedback: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit
): Modifier {
    if (!enabled) return this

    val reducedMotion = LocalReducedMotion.current
    val uiHaptics = rememberUiHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val targetScale = if (isPressed && !reducedMotion) {
        MotionTokens.PressedScale
    } else {
        MotionTokens.SettledScale
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isPressed) MotionTokens.Instant else MotionTokens.Interactive,
        label = "bounceScale"
    )

    return this
        .scale(animatedScale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            role = role,
            onClick = {
                if (hapticFeedback) {
                    uiHaptics.tap()
                }
                onClick()
            }
        )
}

/**
 * Modifier applying only the spring bounce scaling to an existing interaction source.
 */
@Composable
fun Modifier.bounceScale(
    isPressed: Boolean
): Modifier {
    val reducedMotion = LocalReducedMotion.current
    val targetScale = if (isPressed && !reducedMotion) {
        MotionTokens.PressedScale
    } else {
        MotionTokens.SettledScale
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isPressed) MotionTokens.Instant else MotionTokens.Interactive,
        label = "bounceScaleOnly"
    )

    return this.scale(animatedScale)
}
