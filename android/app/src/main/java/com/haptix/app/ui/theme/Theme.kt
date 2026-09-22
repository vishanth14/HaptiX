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
 * Visual theme mode for the HaptiX Cyberpunk research application.
 */
enum class CyberThemeMode {
    DARK,
    LIGHT
}

val LocalCyberThemeMode = staticCompositionLocalOf { CyberThemeMode.DARK }

/**
 * Modern product geometry shapes:
 * - Large cards: 20-24dp
 * - Secondary cards / modules: 16-20dp
 * - Buttons & Inputs: 14-18dp
 */
val CyberShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

/**
 * Cyber Dark Scheme: Deep blue-black laboratory interface with electric cyan and violet accents.
 */
val CyberDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF04101A),
    primaryContainer = CyberCyanContainer,
    onPrimaryContainer = CyberCyanHighlight,
    secondary = CyberViolet,
    onSecondary = Color(0xFF1E0A3C),
    secondaryContainer = CyberVioletContainer,
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = CyberGreen,
    onTertiary = Color(0xFF022C22),
    background = CyberDarkBackground,
    onBackground = CyberTextPrimary,
    surface = CyberDarkSurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberDarkSurfaceElevated,
    onSurfaceVariant = CyberTextSecondary,
    outline = CyberDarkBorder,
    outlineVariant = CyberDarkBorderAccent,
    error = CyberRed,
    onError = Color(0xFF450A0A)
)

/**
 * Cyber Light Scheme: High-contrast daylight technical laboratory interface with electric blue and violet accents.
 */
val CyberLightColorScheme = lightColorScheme(
    primary = CyberBlue,
    onPrimary = Color.White,
    primaryContainer = CyberBlueContainer,
    onPrimaryContainer = CyberBlueDim,
    secondary = CyberLightViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF581C87),
    tertiary = CyberGreen,
    onTertiary = Color.White,
    background = CyberLightBackground,
    onBackground = CyberLightTextPrimary,
    surface = CyberLightSurface,
    onSurface = CyberLightTextPrimary,
    surfaceVariant = CyberLightSurfaceElevated,
    onSurfaceVariant = CyberLightTextSecondary,
    outline = CyberLightBorder,
    outlineVariant = CyberLightBorderAccent,
    error = CyberRed,
    onError = Color.White
)

@Composable
fun HaptiXTheme(
    themeMode: CyberThemeMode = CyberThemeMode.DARK,
    spacing: HaptiXSpacing = HaptiXSpacing(),
    content: @Composable () -> Unit
) {
    val targetScheme = if (themeMode == CyberThemeMode.DARK) CyberDarkColorScheme else CyberLightColorScheme

    // Smooth 250ms color transitions between themes
    val animatedScheme = ColorScheme(
        primary = animateColorAsState(targetScheme.primary, tween(250), label = "primary").value,
        onPrimary = animateColorAsState(targetScheme.onPrimary, tween(250), label = "onPrimary").value,
        primaryContainer = animateColorAsState(targetScheme.primaryContainer, tween(250), label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(targetScheme.onPrimaryContainer, tween(250), label = "onPrimaryContainer").value,
        inversePrimary = targetScheme.inversePrimary,
        secondary = animateColorAsState(targetScheme.secondary, tween(250), label = "secondary").value,
        onSecondary = animateColorAsState(targetScheme.onSecondary, tween(250), label = "onSecondary").value,
        secondaryContainer = animateColorAsState(targetScheme.secondaryContainer, tween(250), label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(targetScheme.onSecondaryContainer, tween(250), label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(targetScheme.tertiary, tween(250), label = "tertiary").value,
        onTertiary = animateColorAsState(targetScheme.onTertiary, tween(250), label = "onTertiary").value,
        tertiaryContainer = targetScheme.tertiaryContainer,
        onTertiaryContainer = targetScheme.onTertiaryContainer,
        background = animateColorAsState(targetScheme.background, tween(250), label = "background").value,
        onBackground = animateColorAsState(targetScheme.onBackground, tween(250), label = "onBackground").value,
        surface = animateColorAsState(targetScheme.surface, tween(250), label = "surface").value,
        onSurface = animateColorAsState(targetScheme.onSurface, tween(250), label = "onSurface").value,
        surfaceVariant = animateColorAsState(targetScheme.surfaceVariant, tween(250), label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(targetScheme.onSurfaceVariant, tween(250), label = "onSurfaceVariant").value,
        surfaceTint = targetScheme.surfaceTint,
        inverseSurface = targetScheme.inverseSurface,
        inverseOnSurface = targetScheme.inverseOnSurface,
        error = animateColorAsState(targetScheme.error, tween(250), label = "error").value,
        onError = animateColorAsState(targetScheme.onError, tween(250), label = "onError").value,
        errorContainer = targetScheme.errorContainer,
        onErrorContainer = targetScheme.onErrorContainer,
        outline = animateColorAsState(targetScheme.outline, tween(250), label = "outline").value,
        outlineVariant = animateColorAsState(targetScheme.outlineVariant, tween(250), label = "outlineVariant").value,
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
        LocalCyberThemeMode provides themeMode
    ) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = Typography,
            shapes = CyberShapes,
            content = content
        )
    }
}
