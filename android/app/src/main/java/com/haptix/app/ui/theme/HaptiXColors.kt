package com.haptix.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * HaptiX Obsidian × Cyberpunk Research Lab Palette.
 *
 * Primary Mode: OBSIDIAN DARK
 * - Primary Signal: Electric Cyan (#00E5FF)
 * - Multimodal Signal: Electric Violet (#7C5CFF)
 * - Active Status: Signal Green (#7CFF6B)
 * - Warning: Amber (#FFB547)
 * - Error: Coral Red (#FF4D67)
 */
object ObsidianDarkPalette {
    val background = Color(0xFF050608)
    val secondaryBackground = Color(0xFF080B10)
    val surface = Color(0xFF0D1118)
    val elevated = Color(0xFF121925)
    val focus = Color(0xFF182131)

    val cyanAccent = Color(0xFF00E5FF)
    val secondaryViolet = Color(0xFF7C5CFF)
    val signalGreen = Color(0xFF7CFF6B)
    val warning = Color(0xFFFFB547)
    val error = Color(0xFFFF4D67)

    val textPrimary = Color(0xFFF5F7FA)
    val textSecondary = Color(0xFFA0AABA)
    val textTertiary = Color(0xFF697586)

    // Borders & Glass
    val borderSubtle = Color(0x17FFFFFF) // rgba(255,255,255,0.09)
    val borderAccent = Color(0x4D00E5FF) // rgba(0,229,255,0.30)
    val glassSurface = Color(0xB8141A26) // rgba(20,26,38,0.72)
    val controlSurface = Color(0x0EFFFFFF) // rgba(255,255,255,0.055)
    val focusSurface = Color(0x1400E5FF) // rgba(0,229,255,0.08)
}

/**
 * HaptiX Light Mode (Technical High-Precision Instrument).
 */
object HaptiXLightPalette {
    val background = Color(0xFFF3F5F8)
    val secondaryBackground = Color(0xFFE9EDF2)
    val surface = Color(0xFFFFFFFF)
    val elevated = Color(0xFFE9EDF2)
    val focus = Color(0xFFDDE3EC)

    val primaryAccent = Color(0xFF007AFF)
    val secondaryAccent = Color(0xFF5E5CE6)
    val signalGreen = Color(0xFF28C840)
    val warning = Color(0xFFFF9500)
    val error = Color(0xFFFF3B30)

    val textPrimary = Color(0xFF11151C)
    val textSecondary = Color(0xFF596575)
    val textTertiary = Color(0xFF8692A2)

    val borderSubtle = Color(0x26000000)
    val borderAccent = Color(0x4D007AFF)
    val glassSurface = Color(0xD9FFFFFF)
    val controlSurface = Color(0x0A000000)
    val focusSurface = Color(0x14007AFF)
}

/**
 * Consolidated theme colors representation for the HaptiX research application.
 */
data class HaptiXColorSystem(
    val background: Color,
    val secondaryBackground: Color,
    val surface: Color,
    val elevated: Color,
    val focus: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val signalGreen: Color,
    val warning: Color,
    val error: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val borderSubtle: Color,
    val borderAccent: Color,
    val glassSurface: Color,
    val controlSurface: Color,
    val focusSurface: Color,
    val isDark: Boolean
)

val DarkColorSystem = HaptiXColorSystem(
    background = ObsidianDarkPalette.background,
    secondaryBackground = ObsidianDarkPalette.secondaryBackground,
    surface = ObsidianDarkPalette.surface,
    elevated = ObsidianDarkPalette.elevated,
    focus = ObsidianDarkPalette.focus,
    accentPrimary = ObsidianDarkPalette.cyanAccent,
    accentSecondary = ObsidianDarkPalette.secondaryViolet,
    signalGreen = ObsidianDarkPalette.signalGreen,
    warning = ObsidianDarkPalette.warning,
    error = ObsidianDarkPalette.error,
    textPrimary = ObsidianDarkPalette.textPrimary,
    textSecondary = ObsidianDarkPalette.textSecondary,
    textTertiary = ObsidianDarkPalette.textTertiary,
    borderSubtle = ObsidianDarkPalette.borderSubtle,
    borderAccent = ObsidianDarkPalette.borderAccent,
    glassSurface = ObsidianDarkPalette.glassSurface,
    controlSurface = ObsidianDarkPalette.controlSurface,
    focusSurface = ObsidianDarkPalette.focusSurface,
    isDark = true
)

val LightColorSystem = HaptiXColorSystem(
    background = HaptiXLightPalette.background,
    secondaryBackground = HaptiXLightPalette.secondaryBackground,
    surface = HaptiXLightPalette.surface,
    elevated = HaptiXLightPalette.elevated,
    focus = HaptiXLightPalette.focus,
    accentPrimary = HaptiXLightPalette.primaryAccent,
    accentSecondary = HaptiXLightPalette.secondaryAccent,
    signalGreen = HaptiXLightPalette.signalGreen,
    warning = HaptiXLightPalette.warning,
    error = HaptiXLightPalette.error,
    textPrimary = HaptiXLightPalette.textPrimary,
    textSecondary = HaptiXLightPalette.textSecondary,
    textTertiary = HaptiXLightPalette.textTertiary,
    borderSubtle = HaptiXLightPalette.borderSubtle,
    borderAccent = HaptiXLightPalette.borderAccent,
    glassSurface = HaptiXLightPalette.glassSurface,
    controlSurface = HaptiXLightPalette.controlSurface,
    focusSurface = HaptiXLightPalette.focusSurface,
    isDark = false
)

val LocalHaptiXColorSystem = staticCompositionLocalOf { DarkColorSystem }

object HaptiXThemeTokens {
    val colors: HaptiXColorSystem
        @Composable
        @ReadOnlyComposable
        get() = LocalHaptiXColorSystem.current
}
