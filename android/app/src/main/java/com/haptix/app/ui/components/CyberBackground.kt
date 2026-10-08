package com.haptix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.haptix.app.ui.components.motion.ElasticOverscrollContainer

/**
 * Calm, minimal background container for HaptiX screens.
 * Provides the base solid surface and activates native elastic rubber-band overscroll.
 */
@Composable
fun HaptiXBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    ElasticOverscrollContainer {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}

/**
 * Backward compatibility alias for CyberBackground.
 */
@Composable
fun CyberBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    HaptiXBackground(modifier = modifier, content = content)
}
