package com.haptix.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.ui.theme.CyberAmber
import com.haptix.app.ui.theme.CyberCyan
import com.haptix.app.ui.theme.CyberGreen
import com.haptix.app.ui.theme.CyberTextMuted
import com.haptix.app.ui.theme.LocalSpacing
import com.haptix.app.ui.theme.TechnicalMicroLabel

import com.haptix.app.haptics.HapticCapabilityLevel

/**
 * Modern 20dp rounded haptic toggle with tactile waveform pulse line visualization,
 * hardware capability verification, and clear distinction between armed and muted states.
 */
@Composable
fun HapticStatusToggle(
    isHapticsEnabled: Boolean,
    onToggleChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isHapticSupported: Boolean = true,
    capabilityLevel: HapticCapabilityLevel = if (isHapticSupported) HapticCapabilityLevel.SUPPORTED else HapticCapabilityLevel.UNAVAILABLE,
    isPlaying: Boolean = false
) {
    val spacing = LocalSpacing.current
    val effectiveEnabled = isHapticsEnabled && isHapticSupported && capabilityLevel != HapticCapabilityLevel.UNAVAILABLE

    val borderColor by animateColorAsState(
        targetValue = if (effectiveEnabled) CyberCyan.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "hapticBorder"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderColor),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HAPTIC ACTUATION",
                        style = TechnicalMicroLabel,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(spacing.xxs))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Indicator Glyph Dot
                        val dotColor = when {
                            capabilityLevel == HapticCapabilityLevel.UNAVAILABLE || !isHapticSupported -> CyberAmber
                            !effectiveEnabled -> CyberTextMuted
                            isPlaying -> CyberGreen
                            capabilityLevel == HapticCapabilityLevel.LIMITED -> CyberAmber
                            else -> CyberCyan
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )

                        Spacer(modifier = Modifier.width(spacing.xs))

                        Column {
                            val titleText = when {
                                capabilityLevel == HapticCapabilityLevel.UNAVAILABLE || !isHapticSupported -> "HAPTICS UNAVAILABLE"
                                !effectiveEnabled -> "HAPTICS OFF"
                                isPlaying -> "HAPTICS PLAYING"
                                capabilityLevel == HapticCapabilityLevel.LIMITED -> "HAPTICS LIMITED"
                                else -> "HAPTICS READY"
                            }
                            val subtitleText = when {
                                capabilityLevel == HapticCapabilityLevel.UNAVAILABLE || !isHapticSupported -> "Actuator hardware not detected"
                                !effectiveEnabled -> "Tactile output muted"
                                isPlaying && capabilityLevel == HapticCapabilityLevel.LIMITED -> "AMPLITUDE FALLBACK ACTIVE"
                                isPlaying -> "FREQUENCY-AWARE STIMULUS ACTIVE"
                                capabilityLevel == HapticCapabilityLevel.LIMITED -> "AMPLITUDE ONLY (NO FREQ MOD)"
                                else -> "ACTUATOR ARMED & READY"
                            }
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.titleMedium,
                                color = when {
                                    capabilityLevel == HapticCapabilityLevel.UNAVAILABLE || !isHapticSupported -> CyberAmber
                                    !effectiveEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                                    isPlaying -> CyberGreen
                                    capabilityLevel == HapticCapabilityLevel.LIMITED -> CyberAmber
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = subtitleText,
                                style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                                color = when {
                                    capabilityLevel == HapticCapabilityLevel.UNAVAILABLE || !isHapticSupported -> CyberAmber.copy(alpha = 0.8f)
                                    !effectiveEnabled -> CyberTextMuted
                                    isPlaying -> CyberGreen
                                    capabilityLevel == HapticCapabilityLevel.LIMITED -> CyberAmber.copy(alpha = 0.8f)
                                    else -> CyberCyan
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(spacing.md))

                Switch(
                    checked = effectiveEnabled,
                    onCheckedChange = { onToggleChanged(it) },
                    enabled = isHapticSupported,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.semantics {
                        this.contentDescription = "Haptic feedback toggle, currently ${if (effectiveEnabled) "On" else "Off"}"
                    }
                )
            }

            Spacer(modifier = Modifier.height(spacing.sm))

            // Subtle Haptic Pulse Line Visualization (● ──●────●──●──)
            HapticPulseLine(isActive = effectiveEnabled)
        }
    }
}

/**
 * Lightweight Canvas representing haptic frequency / node stream: ● ──●────●──●──
 */
@Composable
private fun HapticPulseLine(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val nodeColor = if (isActive) CyberCyan.copy(alpha = 0.85f) else CyberTextMuted.copy(alpha = 0.35f)
    val lineColor = if (isActive) CyberCyan.copy(alpha = 0.45f) else CyberTextMuted.copy(alpha = 0.2f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
    ) {
        val w = size.width
        val midY = size.height / 2f

        // Connecting baseline
        drawLine(
            color = lineColor,
            start = Offset(0f, midY),
            end = Offset(w, midY),
            strokeWidth = 1.5.dp.toPx()
        )

        // Node points along the line
        val nodeFractions = floatArrayOf(0.08f, 0.24f, 0.48f, 0.72f, 0.88f)
        val nodeRadii = floatArrayOf(2.5f, 3.5f, 2.5f, 4f, 2.5f)

        for (i in nodeFractions.indices) {
            val cx = w * nodeFractions[i]
            val r = nodeRadii[i].dp.toPx()
            drawCircle(
                color = nodeColor,
                radius = r,
                center = Offset(cx, midY)
            )
        }
    }
}
