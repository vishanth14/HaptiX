package com.haptix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Calm, minimal background container for HaptiX screens.
 * Replaces previous cyber atmospheric gradients with an elegant, restrained solid surface.
 */
@Composable
fun HaptiXBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
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

