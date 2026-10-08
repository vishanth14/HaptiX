package com.haptix.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.haptix.app.R

/**
 * Obsidian Balthazar Typography Family.
 *
 * Backed by the authentic bundled Balthazar font resource (R.font.balthazar).
 */
val ObsidianBalthazarFontFamily: FontFamily = FontFamily(
    Font(R.font.balthazar, FontWeight.Normal)
)

val TechnicalMonospaceFontFamily: FontFamily = FontFamily.Monospace

val CleanSansFontFamily: FontFamily = FontFamily.SansSerif

/**
 * Editorial and Display Styles powered by Obsidian Balthazar
 */
val ObsidianWordmark = TextStyle(
    fontFamily = ObsidianBalthazarFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 44.sp,
    letterSpacing = (-0.8).sp,
    lineHeight = 48.sp
)

val ObsidianHeroTitle = TextStyle(
    fontFamily = ObsidianBalthazarFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 36.sp,
    letterSpacing = (-0.5).sp,
    lineHeight = 42.sp
)

val ObsidianScreenHeading = TextStyle(
    fontFamily = ObsidianBalthazarFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 30.sp,
    letterSpacing = (-0.3).sp,
    lineHeight = 36.sp
)

val ObsidianSectionHeading = TextStyle(
    fontFamily = ObsidianBalthazarFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    letterSpacing = (-0.2).sp,
    lineHeight = 28.sp
)

/**
 * Technical and System Styles
 */
val TechnicalProtocolLabel = TextStyle(
    fontFamily = TechnicalMonospaceFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
    letterSpacing = 1.4.sp,
    lineHeight = 16.sp
)

val TechnicalReadoutValue = TextStyle(
    fontFamily = TechnicalMonospaceFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    letterSpacing = 0.8.sp,
    lineHeight = 16.sp
)

val TechnicalMicroLabel = TextStyle(
    fontFamily = TechnicalMonospaceFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    letterSpacing = 1.2.sp,
    lineHeight = 14.sp
)

val HaptiXBodyLarge = TextStyle(
    fontFamily = CleanSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    letterSpacing = 0.15.sp,
    lineHeight = 24.sp
)

val HaptiXBodyMedium = TextStyle(
    fontFamily = CleanSansFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    letterSpacing = 0.2.sp,
    lineHeight = 20.sp
)

val HaptiXButtonText = TextStyle(
    fontFamily = CleanSansFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    letterSpacing = 0.8.sp,
    lineHeight = 18.sp
)

/**
 * Standard Material 3 Typography integration with Obsidian Balthazar headings.
 */
val HaptiXTypography = Typography(
    displayLarge = ObsidianWordmark,
    displayMedium = ObsidianHeroTitle,
    displaySmall = ObsidianScreenHeading,
    headlineMedium = ObsidianSectionHeading,
    headlineSmall = TextStyle(
        fontFamily = ObsidianBalthazarFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = HaptiXBodyLarge,
    bodyMedium = HaptiXBodyMedium,
    bodySmall = TextStyle(
        fontFamily = CleanSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = HaptiXButtonText,
    labelMedium = TechnicalReadoutValue,
    labelSmall = TechnicalMicroLabel
)
