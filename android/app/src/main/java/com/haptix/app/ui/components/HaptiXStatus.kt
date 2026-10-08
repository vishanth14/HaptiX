package com.haptix.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.haptics.HapticCapabilityLevel
import com.haptix.app.ui.components.motion.bounceClick
import com.haptix.app.ui.components.motion.breathe
import com.haptix.app.ui.theme.HaptiXBodyMedium
import com.haptix.app.ui.theme.HaptiXShapeTokens
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.MotionTokens
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.TechnicalReadoutValue
import com.haptix.app.ui.theme.rememberUiHaptics

/**
 * Physical Instrument-Style Tactile State Component.
 *
 * Controls tactile actuation output with physical instrument feel.
 * Displays state, frequency, amplitude, and capability status without HUD clutter.
 */
@Composable
fun HaptiXTactileInstrument(
    isHapticsEnabled: Boolean,
    isHapticSupported: Boolean,
    capabilityLevel: HapticCapabilityLevel,
    isPlaying: Boolean,
    currentFrequencyHz: Float?,
    currentAmplitude: Float?,
    onToggleChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiHaptics = rememberUiHaptics()
    val colors = HaptiXThemeTokens.colors
    val isActive = isHapticsEnabled && isHapticSupported

    val (statusLabel, statusColor) = when {
        !isHapticSupported -> "UNAVAILABLE" to colors.textTertiary
        !isHapticsEnabled -> "OFF" to colors.textTertiary
        capabilityLevel == HapticCapabilityLevel.LIMITED -> "LIMITED" to colors.warning
        isPlaying -> "ACTIVE" to colors.accentPrimary
        else -> "READY" to colors.signalGreen
    }

    val animatedDotColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = MotionTokens.colorSpring(),
        label = "statusDotColor"
    )

    val shape = HaptiXShapeTokens.card
    val borderCol = if (isActive && isPlaying) colors.accentPrimary else colors.borderSubtle

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, borderCol, shape)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main control row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Status beacon & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .breathe(
                                enabled = isActive,
                                durationMs = if (isPlaying) 1500 else 2800,
                                minScale = 0.90f,
                                maxScale = 1.15f
                            )
                            .background(animatedDotColor)
                    )

                    Column {
                        Text(
                            text = "TACTILE OUTPUT",
                            style = TechnicalMicroLabel,
                            color = colors.accentPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = statusLabel,
                                style = TechnicalReadoutValue,
                                color = statusColor
                            )
                            if (capabilityLevel == HapticCapabilityLevel.LIMITED && isHapticSupported && isHapticsEnabled) {
                                Text(
                                    text = "• AMPLITUDE MODE",
                                    style = TechnicalMicroLabel.copy(fontSize = 9.sp),
                                    color = colors.warning
                                )
                            }
                        }
                    }
                }

                // Right: Physical instrument rocker toggle button
                val toggleBg = if (isActive) colors.focusSurface else colors.controlSurface
                val toggleBorder = if (isActive) colors.accentPrimary else colors.borderSubtle

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(HaptiXShapeTokens.input)
                        .background(toggleBg)
                        .border(1.dp, toggleBorder, HaptiXShapeTokens.input)
                        .bounceClick(
                            enabled = isHapticSupported,
                            role = Role.Switch,
                            onClick = {
                                if (isHapticSupported) {
                                    uiHaptics.selection()
                                    onToggleChanged(!isHapticsEnabled)
                                }
                            }
                        )
                        .semantics {
                            this.role = Role.Switch
                            this.contentDescription = "Toggle tactile output, currently ${if (isHapticsEnabled) "On" else "Off"}"
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isHapticsEnabled) "HAPTICS [ ON ]" else "HAPTICS [ OFF ]",
                        style = TechnicalMicroLabel,
                        color = if (isHapticsEnabled) colors.accentPrimary else colors.textSecondary
                    )
                }
            }

            // Research Telemetry strip (Frequency & Amplitude)
            if (isActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(colors.borderSubtle)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "HAPTIC FREQUENCY",
                            style = TechnicalMicroLabel,
                            color = colors.textTertiary
                        )
                        val freqText = currentFrequencyHz?.let { String.format(java.util.Locale.US, "%.0f Hz", it) } ?: "---"
                        Text(
                            text = freqText,
                            style = TechnicalReadoutValue,
                            color = colors.textPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "AMPLITUDE",
                            style = TechnicalMicroLabel,
                            color = colors.textTertiary
                        )
                        val ampText = currentAmplitude?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "---"
                        Text(
                            text = ampText,
                            style = TechnicalReadoutValue,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}
