package com.haptix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.haptix.app.ui.theme.HaptiXShapeTokens
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.TechnicalProtocolLabel

/**
 * Technical protocol eyebrow label (e.g. "HAPTIX // RESEARCH PROTOCOL", "01 // PARTICIPANT PROFILE").
 */
@Composable
fun HaptiXProtocolEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = HaptiXThemeTokens.colors.accentPrimary
) {
    Text(
        text = text,
        style = TechnicalProtocolLabel,
        color = color,
        modifier = modifier
    )
}

/**
 * Compact technical telemetry badge (e.g. "180 Hz", "TACTILE ACTIVE", "MM:SS").
 */
@Composable
fun HaptiXTelemetryBadge(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    textColor: Color = if (accent) HaptiXThemeTokens.colors.accentPrimary else HaptiXThemeTokens.colors.textSecondary
) {
    val bgColor = if (accent) HaptiXThemeTokens.colors.focusSurface else HaptiXThemeTokens.colors.controlSurface
    val borderColor = if (accent) HaptiXThemeTokens.colors.borderAccent else HaptiXThemeTokens.colors.borderSubtle

    Box(
        modifier = modifier
            .clip(HaptiXShapeTokens.micro)
            .background(bgColor)
            .border(0.5.dp, borderColor, HaptiXShapeTokens.micro)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = TechnicalMicroLabel,
            color = textColor
        )
    }
}
