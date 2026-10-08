package com.haptix.app.ui.components.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import com.haptix.app.ui.theme.LocalReducedMotion
import com.haptix.app.ui.theme.MotionTokens

/**
 * Modifier applying Apple-style restrained lensing (subtle focus depth).
 *
 * Normal: scale 1.0
 * Focused / Selected: scale 1.015
 * Pressed: scale 0.975
 *
 * Provides tactile spatial hierarchy without text distortion or extreme magnification.
 * Respects [LocalReducedMotion].
 */
@Composable
fun Modifier.lensing(
    interactionSource: MutableInteractionSource,
    isSelected: Boolean = false
): Modifier {
    val reducedMotion = LocalReducedMotion.current
    if (reducedMotion) return this

    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val targetScale = when {
        isPressed -> 0.975f
        isFocused || isSelected -> MotionTokens.LensingScale
        else -> MotionTokens.SettledScale
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isPressed) MotionTokens.Instant else MotionTokens.Interactive,
        label = "lensingScale"
    )

    return this.scale(scale)
}
