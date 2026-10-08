package com.haptix.app.ui.components.motion

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.haptix.app.ui.theme.LocalReducedMotion

/**
 * Applies Android's native elastic/rubber-band overscroll effect to scrollable containers.
 * In Android 12+ (API 31+), the platform provides stretch/rubber-band overscroll by default.
 * This container ensures the native elastic overscroll configuration is active.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ElasticOverscrollContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val config = if (reducedMotion) {
        null
    } else {
        OverscrollConfiguration(
            glowColor = Color.Transparent,
            drawPadding = PaddingValues()
        )
    }

    CompositionLocalProvider(
        LocalOverscrollConfiguration provides config,
        content = content
    )
}

/**
 * Modifier extension to apply the native platform overscroll effect directly.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.elasticOverscroll(): Modifier {
    val reducedMotion = LocalReducedMotion.current
    if (reducedMotion) return this

    val effect = ScrollableDefaults.overscrollEffect()
    return this.overscroll(effect)
}
