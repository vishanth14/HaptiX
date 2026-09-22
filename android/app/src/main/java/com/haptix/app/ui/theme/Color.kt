package com.haptix.app.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// APPLE-INSPIRED COLOR SYSTEM (HaptiX Research Edition)
// =========================================================================

// --- Dark Mode ---
val AppleDarkBackground = Color(0xFF000000)
val AppleDarkSurfacePrimary = Color(0xFF0B0B0D)
val AppleDarkSurfaceSecondary = Color(0xFF151518)
val AppleDarkSurfaceTertiary = Color(0xFF1E1E22)
val AppleDarkBorder = Color(0xFF2C2C2E)
val AppleDarkBorderSubtle = Color(0xFF1C1C1E)

val AppleDarkTextPrimary = Color(0xFFF5F5F7)
val AppleDarkTextSecondary = Color(0xFFA1A1A6)
val AppleDarkTextTertiary = Color(0xFF6E6E73)

val AppleSystemBlueDark = Color(0xFF0A84FF)
val AppleSystemPurpleDark = Color(0xFF5E5CE6)
val AppleSystemGreenDark = Color(0xFF30D158)
val AppleSystemYellowDark = Color(0xFFFFD60A)
val AppleSystemRedDark = Color(0xFFFF453A)

// --- Light Mode ---
val AppleLightBackground = Color(0xFFF5F5F7)
val AppleLightSurfacePrimary = Color(0xFFFFFFFF)
val AppleLightSurfaceSecondary = Color(0xFFEDEDF0)
val AppleLightSurfaceTertiary = Color(0xFFE5E5EA)
val AppleLightBorder = Color(0xFFD1D1D6)
val AppleLightBorderSubtle = Color(0xFFE5E5EA)

val AppleLightTextPrimary = Color(0xFF1D1D1F)
val AppleLightTextSecondary = Color(0xFF6E6E73)
val AppleLightTextTertiary = Color(0xFF86868B)

val AppleSystemBlueLight = Color(0xFF007AFF)
val AppleSystemPurpleLight = Color(0xFF5856D6)
val AppleSystemGreenLight = Color(0xFF34C759)
val AppleSystemYellowLight = Color(0xFFFFCC00)
val AppleSystemRedLight = Color(0xFFFF3B30)

// =========================================================================
// HAPTIX DESIGN SYSTEM TOKENS
// =========================================================================
val HaptiXBlue = AppleSystemBlueDark
val HaptiXBlueLight = AppleSystemBlueLight
val HaptiXPurple = AppleSystemPurpleDark
val HaptiXGreen = AppleSystemGreenDark
val HaptiXYellow = AppleSystemYellowDark
val HaptiXRed = AppleSystemRedDark

// Star Rating Colors
val StarGold = Color(0xFFFFD60A)
val StarEmptyDark = Color(0xFF3A3A3C)
val StarEmptyLight = Color(0xFFD1D1D6)
val StarEmpty = StarEmptyDark

// =========================================================================
// BACKWARD-COMPATIBLE ALIASES (Transitioning from Cyberpunk to Apple)
// =========================================================================
val CyberDarkBackground = AppleDarkBackground
val CyberDarkSurface = AppleDarkSurfacePrimary
val CyberDarkSurfaceElevated = AppleDarkSurfaceSecondary
val CyberDarkSurfaceElevatedSecondary = AppleDarkSurfaceTertiary
val CyberDarkBorder = AppleDarkBorder
val CyberDarkBorderAccent = AppleDarkBorderSubtle

val CyberCyan = AppleSystemBlueDark
val CyberCyanDim = Color(0xFF0066CC)
val CyberCyanContainer = Color(0x1F0A84FF)
val CyberCyanGlow = Color(0x1A0A84FF)
val CyberCyanHighlight = Color(0xFFBCE3FF)

val CyberViolet = AppleSystemPurpleDark
val CyberVioletDim = Color(0xFF4B49C8)
val CyberVioletContainer = Color(0x1F5E5CE6)
val CyberVioletGlow = Color(0x1A5E5CE6)

val CyberTextPrimary = AppleDarkTextPrimary
val CyberTextSecondary = AppleDarkTextSecondary
val CyberTextMuted = AppleDarkTextTertiary

val CyberLightBackground = AppleLightBackground
val CyberLightSurface = AppleLightSurfacePrimary
val CyberLightSurfaceElevated = AppleLightSurfaceSecondary
val CyberLightSurfaceElevatedSecondary = AppleLightSurfaceTertiary
val CyberLightBorder = AppleLightBorder
val CyberLightBorderAccent = AppleLightBorderSubtle

val CyberBlue = AppleSystemBlueLight
val CyberLightCyan = AppleSystemBlueLight
val CyberBlueDim = Color(0xFF0056B3)
val CyberBlueContainer = Color(0x1A007AFF)
val CyberBlueGlow = Color(0x1A007AFF)

val CyberLightViolet = AppleSystemPurpleLight
val CyberLightTextPrimary = AppleLightTextPrimary
val CyberLightTextSecondary = AppleLightTextSecondary
val CyberLightTextMuted = AppleLightTextTertiary

val CyberGreen = AppleSystemGreenDark
val CyberGreenGlow = Color(0x1A30D158)
val CyberGreenContainer = Color(0x1A30D158)

val CyberAmber = AppleSystemYellowDark
val CyberAmberContainer = Color(0x1AFFD60A)

val CyberRed = AppleSystemRedDark
val CyberRedContainer = Color(0x1AFF453A)

val CyberStarGold = StarGold

val Slate950 = AppleDarkBackground
val Slate900 = Color(0xFF0C0C0E)
val Slate800 = AppleDarkSurfaceSecondary
val Slate700 = Color(0xFF2C2C2E)
val Slate600 = Color(0xFF3A3A3C)
val Slate500 = Color(0xFF6E6E73)
val Slate400 = Color(0xFF8E8E93)
val Slate300 = Color(0xFFAEAEB2)
val Slate200 = Color(0xFFD1D1D6)
val Slate100 = Color(0xFFE5E5EA)
val Slate50 = Color(0xFFF2F2F7)

val AccentBlue = AppleSystemBlueLight
val AccentBlueLight = AppleSystemBlueDark
val AccentBlueDark = Color(0xFF0056B3)
val AccentBlueContainer = Color(0x1A007AFF)
val AccentBlueBorder = Color(0xFFBCE3FF)

val StatusGreen = AppleSystemGreenDark
val StatusGreenContainer = Color(0x1A30D158)
val StatusRed = AppleSystemRedDark
val StatusRedContainer = Color(0x1AFF453A)
val StatusAmber = AppleSystemYellowDark
val StatusAmberContainer = Color(0x1AFFD60A)

val SurfaceLight = AppleLightSurfacePrimary
val SurfaceDark = AppleDarkSurfacePrimary
val SurfaceVariantLight = AppleLightSurfaceSecondary
val SurfaceVariantDark = AppleDarkSurfaceSecondary

val BackgroundLight = CyberLightBackground
val BackgroundDark = CyberDarkBackground
val BorderLight = CyberLightBorder
val BorderDark = CyberDarkBorder
