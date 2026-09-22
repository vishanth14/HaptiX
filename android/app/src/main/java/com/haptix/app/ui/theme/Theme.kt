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
 * Apple-inspired Dark Scheme:
 * Deep black backdrop (#000000), subtle charcoal surfaces (#0B0B0D, #151518),
 * pristine light typography (#F5F5F7), and restrained system blue (#0A84FF).
 */
val HaptiXDarkColorScheme = darkColorScheme(
    primary = AppleSystemBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0x1F0A84FF),
    onPrimaryContainer = Color(0xFFBCE3FF),
    secondary = AppleSystemPurpleDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0x1F5E5CE6),
    onSecondaryContainer = Color(0xFFE5E4FF),
    tertiary = AppleSystemGreenDark,
    onTertiary = Color.White,
    background = AppleDarkBackground,
    onBackground = AppleDarkTextPrimary,
    surface = AppleDarkSurfacePrimary,
    onSurface = AppleDarkTextPrimary,
    surfaceVariant = AppleDarkSurfaceSecondary,
    onSurfaceVariant = AppleDarkTextSecondary,
    outline = AppleDarkBorder,
    outlineVariant = AppleDarkBorderSubtle,
    error = AppleSystemRedDark,
    onError = Color.White
)

/**
 * Apple-inspired Light Scheme:
 * Clean soft gray backdrop (#F5F5F7), pure white cards (#FFFFFF), subtle secondary gray (#EDEDF0),
 * crisp dark typography (#1D1D1F), and vibrant system blue (#007AFF).
 */
val HaptiXLightColorScheme = lightColorScheme(
    primary = AppleSystemBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0x1A007AFF),
    onPrimaryContainer = Color(0xFF0056B3),
    secondary = AppleSystemPurpleLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0x1A5856D6),
    onSecondaryContainer = Color(0xFF3836B0),
    tertiary = AppleSystemGreenLight,
    onTertiary = Color.White,
    background = AppleLightBackground,
    onBackground = AppleLightTextPrimary,
    surface = AppleLightSurfacePrimary,
    onSurface = AppleLightTextPrimary,
    surfaceVariant = AppleLightSurfaceSecondary,
    onSurfaceVariant = AppleLightTextSecondary,
    outline = AppleLightBorder,
    outlineVariant = AppleLightBorderSubtle,
    error = AppleSystemRedLight,
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
        LocalCyberThemeMode provides themeMode
    ) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = Typography,
            shapes = HaptiXShapes,
            content = content
        )
    }
}

