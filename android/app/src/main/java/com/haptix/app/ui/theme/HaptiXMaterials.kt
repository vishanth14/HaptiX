package com.haptix.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Translucency and material level classification for HaptiX instrumentation.
 */
enum class HaptiXMaterialLevel {
    OBSIDIAN,
    ELEVATED,
    GLASS,
    VIBRANT,
    CONTROL,
    FOCUS
}

/**
 * Material tokens defining surfaces, borders, and depths.
 */
object HaptiXMaterialTokens {
    val HairlineBorderWidth: Dp = 1.dp
    val SubtleBorderWidth: Dp = 0.5.dp

    @Composable
    @ReadOnlyComposable
    fun surfaceColor(level: HaptiXMaterialLevel): Color {
        val colors = LocalHaptiXColorSystem.current
        return when (level) {
            HaptiXMaterialLevel.OBSIDIAN -> colors.surface
            HaptiXMaterialLevel.ELEVATED -> colors.elevated
            HaptiXMaterialLevel.GLASS -> colors.glassSurface
            HaptiXMaterialLevel.VIBRANT -> if (colors.isDark) Color(0x3300E5FF) else Color(0x1A007AFF)
            HaptiXMaterialLevel.CONTROL -> colors.controlSurface
            HaptiXMaterialLevel.FOCUS -> colors.focusSurface
        }
    }

    @Composable
    @ReadOnlyComposable
    fun borderColor(accent: Boolean = false): Color {
        val colors = LocalHaptiXColorSystem.current
        return if (accent) colors.borderAccent else colors.borderSubtle
    }

    @Composable
    @ReadOnlyComposable
    fun borderStroke(
        width: Dp = HairlineBorderWidth,
        accent: Boolean = false
    ): BorderStroke {
        return BorderStroke(width, borderColor(accent))
    }
}

/**
 * Obsidian / Glass panel container for structured research modules.
 */
@Composable
fun HaptiXGlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = HaptiXShapeTokens.card,
    level: HaptiXMaterialLevel = HaptiXMaterialLevel.GLASS,
    accentBorder: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(HaptiXMaterialTokens.surfaceColor(level))
            .border(HaptiXMaterialTokens.borderStroke(accent = accentBorder), shape)
    ) {
        content()
    }
}
