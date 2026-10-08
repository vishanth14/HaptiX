package com.haptix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.ui.components.motion.MagicIcon
import com.haptix.app.ui.components.motion.bounceClick
import com.haptix.app.ui.theme.CyberThemeMode
import com.haptix.app.ui.theme.HaptiXShapeTokens
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.LocalCyberThemeMode
import com.haptix.app.ui.theme.ObsidianBalthazarFontFamily
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.rememberUiHaptics

/**
 * Minimal Cyberpunk Research Console Header.
 *
 * Displays research instrument identity, active status beacon, and compact theme toggle.
 * Replaces generic white/gray Material toolbars.
 */
@Composable
fun HaptiXHeader(
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "HaptiX",
    subtitle: String = "MULTIMODAL HAPTIC RESEARCH",
    statusText: String = "READY",
    isStatusActive: Boolean = true
) {
    val currentTheme = LocalCyberThemeMode.current
    val isDark = currentTheme == CyberThemeMode.DARK
    val colors = HaptiXThemeTokens.colors
    val uiHaptics = rememberUiHaptics()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Instrument Branding & Protocol Context
            Column {
                Text(
                    text = title,
                    fontFamily = ObsidianBalthazarFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = (-0.3).sp,
                    color = colors.textPrimary
                )
                Text(
                    text = subtitle,
                    style = TechnicalMicroLabel,
                    color = colors.accentPrimary
                )
            }

            // Right: Telemetry Status Beacon & Compact Instrument Theme Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HaptiXSignalBeacon(
                    label = statusText,
                    isActive = isStatusActive,
                    activeColor = colors.signalGreen
                )

                // Compact circular control for theme switching
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.controlSurface)
                        .border(0.5.dp, colors.borderSubtle, CircleShape)
                        .bounceClick(
                            role = Role.Switch,
                            onClick = {
                                uiHaptics.selection()
                                onToggleTheme()
                            }
                        )
                        .semantics {
                            this.role = Role.Switch
                            this.contentDescription = "Switch to ${if (isDark) "Light" else "Dark"} Mode"
                        }
                ) {
                    MagicIcon(targetState = isDark) { dark ->
                        Text(
                            text = if (dark) "◐" else "◑",
                            fontSize = 14.sp,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }

        // Hairline bottom separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.borderSubtle)
        )
    }
}
