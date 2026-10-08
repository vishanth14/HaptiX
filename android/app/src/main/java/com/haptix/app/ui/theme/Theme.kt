package com.haptix.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Visual theme mode for the HaptiX research application.
 */
enum class CyberThemeMode {
    DARK,
    LIGHT
}

typealias HaptiXThemeMode = CyberThemeMode

val LocalCyberThemeMode = staticCompositionLocalOf { CyberThemeMode.DARK }

/**
 * Apple-inspired soft rounded geometry shapes:
 * - Small / Chips: 12dp
 * - Medium / Cards & Inputs: 18dp
 * - Large / Media & Featured modules: 22dp
 * - ExtraLarge / Hero artwork: 28dp
 */
val HaptiXShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val CyberShapes = HaptiXShapes

/**
 * Obsidian Dark Scheme:
 * Deep obsidian backdrop (#050608), technical dark surfaces (#0D1118, #121925),
 * pristine light typography (#F5F7FA), and electric cyan primary signal (#00E5FF).
 */
val HaptiXDarkColorScheme = darkColorScheme(
    primary = ObsidianDarkPalette.cyanAccent,
    onPrimary = ObsidianDarkPalette.background,
    primaryContainer = Color(0x3300E5FF),
    onPrimaryContainer = Color(0xFFBCE3FF),
    secondary = ObsidianDarkPalette.secondaryViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0x337C5CFF),
    onSecondaryContainer = Color(0xFFE5E4FF),
    tertiary = ObsidianDarkPalette.signalGreen,
    onTertiary = ObsidianDarkPalette.background,
    background = ObsidianDarkPalette.background,
    onBackground = ObsidianDarkPalette.textPrimary,
    surface = ObsidianDarkPalette.surface,
    onSurface = ObsidianDarkPalette.textPrimary,
    surfaceVariant = ObsidianDarkPalette.elevated,
    onSurfaceVariant = ObsidianDarkPalette.textSecondary,
    outline = ObsidianDarkPalette.borderSubtle,
    outlineVariant = ObsidianDarkPalette.borderAccent,
    error = ObsidianDarkPalette.error,
    onError = Color.White
)

/**
 * Light Scheme:
 * Clean high-precision gray backdrop (#F3F5F8), pure white cards (#FFFFFF), subtle secondary gray (#E9EDF2),
 * crisp dark typography (#11151C), and vibrant scientific blue (#007AFF).
 */
val HaptiXLightColorScheme = lightColorScheme(
    primary = HaptiXLightPalette.primaryAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0x1A007AFF),
    onPrimaryContainer = Color(0xFF0056B3),
    secondary = HaptiXLightPalette.secondaryAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0x1A5E5CE6),
    onSecondaryContainer = Color(0xFF3836B0),
    tertiary = HaptiXLightPalette.signalGreen,
    onTertiary = Color.White,
    background = HaptiXLightPalette.background,
    onBackground = HaptiXLightPalette.textPrimary,
    surface = HaptiXLightPalette.surface,
    onSurface = HaptiXLightPalette.textPrimary,
    surfaceVariant = HaptiXLightPalette.elevated,
    onSurfaceVariant = HaptiXLightPalette.textSecondary,
    outline = HaptiXLightPalette.borderSubtle,
    outlineVariant = HaptiXLightPalette.borderAccent,
    error = HaptiXLightPalette.error,
    onError = Color.White
)

val CyberDarkColorScheme = HaptiXDarkColorScheme
val CyberLightColorScheme = HaptiXLightColorScheme

@Composable
fun HaptiXTheme(
    themeMode: CyberThemeMode = CyberThemeMode.DARK,
    spacing: HaptiXSpacing = HaptiXSpacing(),
    content: @Composable () -> Unit
) {
    val targetScheme = if (themeMode == CyberThemeMode.DARK) HaptiXDarkColorScheme else HaptiXLightColorScheme
    val colorSystem = if (themeMode == CyberThemeMode.DARK) DarkColorSystem else LightColorSystem

    // Smooth 200ms color transitions between dark and light themes
    val animatedScheme = ColorScheme(
        primary = animateColorAsState(targetScheme.primary, tween(200), label = "primary").value,
        onPrimary = animateColorAsState(targetScheme.onPrimary, tween(200), label = "onPrimary").value,
        primaryContainer = animateColorAsState(targetScheme.primaryContainer, tween(200), label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(targetScheme.onPrimaryContainer, tween(200), label = "onPrimaryContainer").value,
        inversePrimary = targetScheme.inversePrimary,
        secondary = animateColorAsState(targetScheme.secondary, tween(200), label = "secondary").value,
        onSecondary = animateColorAsState(targetScheme.onSecondary, tween(200), label = "onSecondary").value,
        secondaryContainer = animateColorAsState(targetScheme.secondaryContainer, tween(200), label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(targetScheme.onSecondaryContainer, tween(200), label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(targetScheme.tertiary, tween(200), label = "tertiary").value,
        onTertiary = animateColorAsState(targetScheme.onTertiary, tween(200), label = "onTertiary").value,
        tertiaryContainer = targetScheme.tertiaryContainer,
        onTertiaryContainer = targetScheme.onTertiaryContainer,
        background = animateColorAsState(targetScheme.background, tween(200), label = "background").value,
        onBackground = animateColorAsState(targetScheme.onBackground, tween(200), label = "onBackground").value,
        surface = animateColorAsState(targetScheme.surface, tween(200), label = "surface").value,
        onSurface = animateColorAsState(targetScheme.onSurface, tween(200), label = "onSurface").value,
        surfaceVariant = animateColorAsState(targetScheme.surfaceVariant, tween(200), label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(targetScheme.onSurfaceVariant, tween(200), label = "onSurfaceVariant").value,
        surfaceTint = targetScheme.surfaceTint,
        inverseSurface = targetScheme.inverseSurface,
        inverseOnSurface = targetScheme.inverseOnSurface,
        error = animateColorAsState(targetScheme.error, tween(200), label = "error").value,
        onError = animateColorAsState(targetScheme.onError, tween(200), label = "onError").value,
        errorContainer = targetScheme.errorContainer,
        onErrorContainer = targetScheme.onErrorContainer,
        outline = animateColorAsState(targetScheme.outline, tween(200), label = "outline").value,
        outlineVariant = animateColorAsState(targetScheme.outlineVariant, tween(200), label = "outlineVariant").value,
        scrim = targetScheme.scrim,
        surfaceBright = targetScheme.surfaceBright,
        surfaceDim = targetScheme.surfaceDim,
        surfaceContainer = targetScheme.surfaceContainer,
        surfaceContainerHigh = targetScheme.surfaceContainerHigh,
        surfaceContainerHighest = targetScheme.surfaceContainerHighest,
        surfaceContainerLow = targetScheme.surfaceContainerLow,
        surfaceContainerLowest = targetScheme.surfaceContainerLowest
    )

    CompositionLocalProvider(
        LocalSpacing provides spacing,
        LocalCyberThemeMode provides themeMode,
        LocalHaptiXColorSystem provides colorSystem
    ) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = HaptiXTypography,
            shapes = HaptiXShapes,
            content = content
        )
    }
}

