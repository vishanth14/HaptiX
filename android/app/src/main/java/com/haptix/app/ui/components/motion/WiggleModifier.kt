package com.haptix.app.ui.components.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.haptix.app.ui.theme.LocalReducedMotion
import kotlin.math.roundToInt

/**
 * Controller for imperatively triggering a non-continuous Wiggle oscillation.
 */
class WiggleState {
    var triggerCount by mutableLongStateOf(0L)
        private set

    fun trigger() {
        triggerCount++
    }
}

@Composable
fun rememberWiggleState(): WiggleState {
    return remember { WiggleState() }
}

/**
 * Modifier that applies a brief Apple-style horizontal oscillation ("wiggle")
 * when [wiggleState] triggers.
 *
 * Sequence: 0 -> -6dp -> +6dp -> -4dp -> +4dp -> 0dp over 320ms.
 * Suppressed if [LocalReducedMotion] is true.
 */
@Composable
fun Modifier.wiggle(
    wiggleState: WiggleState,
    onAnimationEnd: () -> Unit = {}
): Modifier {
    val reducedMotion = LocalReducedMotion.current
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(wiggleState.triggerCount) {
        if (wiggleState.triggerCount > 0L && !reducedMotion) {
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 320
                    0f at 0
                    -6f at 50
                    6f at 120
                    -4f at 190
                    4f at 260
                    0f at 320
                }
            )
            onAnimationEnd()
        }
    }

    return this.offset { IntOffset(x = offsetX.value.dp.roundToPx(), y = 0) }
}
