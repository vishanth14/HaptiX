package com.haptix.app.ui.components.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Reusable icon container that performs smooth Apple-style glyph morphing
 * (scale + fade continuous transition) between icon states rather than abrupt replacements.
 *
 * Example use cases:
 * - Play ↔ Pause
 * - Volume ↔ Muted
 * - Fullscreen ↔ Windowed
 * - Haptics ON ↔ OFF
 * - Light ↔ Dark
 */
@Composable
fun <T> MagicIcon(
    targetState: T,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable (T) -> Unit
) {
    Box(
        modifier = modifier,
        contentAlignment = contentAlignment
    ) {
        AnimatedContent(
            targetState = targetState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(140)) + scaleIn(initialScale = 0.85f, animationSpec = tween(140)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.85f, animationSpec = tween(120))
                    )
            },
            label = "magicIconTransition"
        ) { state ->
            content(state)
        }
    }
}
