package com.haptix.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Top navigation bar for HaptiX research application.
 * Directly renders [HaptiXHeader].
 */
@Composable
fun HaptiXTopBar(
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "HaptiX",
    subtitle: String = "MULTIMODAL HAPTIC RESEARCH"
) {
    HaptiXHeader(
        onToggleTheme = onToggleTheme,
        modifier = modifier,
        title = title,
        subtitle = subtitle
    )
}

/**
 * Backward compatibility alias for CyberTopBar.
 */
@Composable
fun CyberTopBar(
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "HaptiX",
    subtitle: String = "MULTIMODAL HAPTIC RESEARCH"
) {
    HaptiXTopBar(
        onToggleTheme = onToggleTheme,
        modifier = modifier,
        title = title,
        subtitle = subtitle
    )
}
