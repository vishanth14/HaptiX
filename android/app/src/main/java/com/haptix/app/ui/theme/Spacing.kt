package com.haptix.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standardized spacing tokens for the HaptiX research application.
 * Follows Apple-inspired spacious layouts with generous whitespace.
 */
@Immutable
data class HaptiXSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
    // Editorial layout tokens
    val screenHorizontal: Dp = 22.dp,
    val sectionGap: Dp = 32.dp,
    val cardGap: Dp = 16.dp,
    val largeSectionGap: Dp = 48.dp
)

val LocalSpacing = staticCompositionLocalOf { HaptiXSpacing() }

