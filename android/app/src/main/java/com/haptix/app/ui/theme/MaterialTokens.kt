package com.haptix.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material surface translucency levels inspired by modern Apple system materials.
 * Provides restrained tonal translucency, low-opacity surface overlays,
 * and subtle hairline borders for Dark and Light modes.
 */
enum class MaterialLevel {
    UltraThin,
    Thin,
    Regular,
    Thick,
    Chrome
}

object MaterialTokens {
    /**
     * Hairline border width for glass/material containers.
     * Subtle 0.5dp definition rather than heavy borders.
     */
    val HairlineBorderWidth: Dp = 0.5.dp

    /**
     * Soft corner radii for material surfaces.
     */
    val SmallShapeRadius: Dp = 12.dp
    val MediumShapeRadius: Dp = 18.dp
    val LargeShapeRadius: Dp = 22.dp
    val PillShapeRadius: Dp = 28.dp

    /**
     * Returns the appropriate translucent surface color based on [MaterialLevel] and theme.
     */
    @Composable
    fun surfaceColor(level: MaterialLevel, isDark: Boolean = isSystemInDarkTheme()): Color {
        return if (isDark) {
            when (level) {
                MaterialLevel.UltraThin -> Color(0xFF151518).copy(alpha = 0.45f)
                MaterialLevel.Thin -> Color(0xFF151518).copy(alpha = 0.65f)
                MaterialLevel.Regular -> Color(0xFF151518).copy(alpha = 0.82f)
                MaterialLevel.Thick -> Color(0xFF1E1E22).copy(alpha = 0.92f)
                MaterialLevel.Chrome -> Color(0xFF0B0B0D).copy(alpha = 0.88f)
            }
        } else {
            when (level) {
                MaterialLevel.UltraThin -> Color(0xFFFFFFFF).copy(alpha = 0.55f)
                MaterialLevel.Thin -> Color(0xFFFFFFFF).copy(alpha = 0.72f)
                MaterialLevel.Regular -> Color(0xFFFFFFFF).copy(alpha = 0.86f)
                MaterialLevel.Thick -> Color(0xFFEDEDF0).copy(alpha = 0.94f)
                MaterialLevel.Chrome -> Color(0xFFF5F5F7).copy(alpha = 0.90f)
            }
        }
    }

    /**
     * Returns the subtle hairline border color for translucent containers.
     */
    @Composable
    fun hairlineBorderColor(isDark: Boolean = isSystemInDarkTheme()): Color {
        return if (isDark) {
            Color.White.copy(alpha = 0.12f)
        } else {
            Color.Black.copy(alpha = 0.08f)
        }
    }

    /**
     * Returns a subtle top highlight color for layered material depth.
     */
    @Composable
    fun surfaceHighlightColor(isDark: Boolean = isSystemInDarkTheme()): Color {
        return if (isDark) {
            Color.White.copy(alpha = 0.06f)
        } else {
            Color.White.copy(alpha = 0.40f)
        }
    }
}
