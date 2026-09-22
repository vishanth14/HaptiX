package com.haptix.app.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.haptics.HapticCapabilityLevel
import com.haptix.app.ui.theme.AppleSystemGreenDark
import com.haptix.app.ui.theme.AppleSystemYellowDark
import com.haptix.app.ui.theme.LocalSpacing

/**
 * Clean native-style Apple-inspired Haptic control card.
 * Presents a clear toggle for tactile feedback with a restrained status dot and device capability explanation.
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

    // Clean status indicator dot & label
    val (dotColor, statusTitle, statusSubtitle) = when {
        !isHapticSupported || capabilityLevel == HapticCapabilityLevel.UNAVAILABLE -> Triple(
            Color(0xFF8E8E93),
            "Haptics Unavailable",
            "This device does not support vibration."
        )
        !effectiveEnabled -> Triple(
            Color(0xFF8E8E93),
            "Haptics Off",
            "Tactile feedback muted for this session."
        )
        capabilityLevel == HapticCapabilityLevel.LIMITED -> Triple(
            AppleSystemYellowDark,
            "Haptics Limited",
            "Frequency control unavailable on this device."
        )
        isPlaying -> Triple(
            AppleSystemGreenDark,
            "Haptics Playing",
            "Tactile actuation active."
        )
        else -> Triple(
            AppleSystemGreenDark,
            "Haptics Ready",
            "Synchronized tactile feedback enabled."
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Main Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Haptics",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Haptic feedback during playback",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Switch(
                    checked = isHapticsEnabled && isHapticSupported,
                    onCheckedChange = { if (isHapticSupported) onToggleChanged(it) },
                    enabled = isHapticSupported,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        uncheckedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.semantics {
                        this.contentDescription = "Toggle haptic tactile feedback"
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hairline separator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtle Status Sub-row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = statusTitle,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = statusSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


