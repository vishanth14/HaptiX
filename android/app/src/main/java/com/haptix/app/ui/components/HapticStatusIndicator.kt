package com.haptix.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.haptix.app.ui.theme.CyberAmber
import com.haptix.app.ui.theme.CyberCyan
import com.haptix.app.ui.theme.CyberGreen
import com.haptix.app.ui.theme.CyberTextMuted
import com.haptix.app.ui.theme.LocalSpacing
import com.haptix.app.ui.theme.TechnicalMicroLabel

/**
 * Concrete hardware and playback states for the tactile actuator.
 */
enum class HapticActuatorState {
    UNAVAILABLE,
    OFF,
    READY,
    PLAYING
}

/**
 * Reusable laboratory haptic status indicator.
 * Accurately communicates real actuator hardware readiness without fabricating frequency playback.
 */
@Composable
fun HapticStatusIndicator(
    state: HapticActuatorState,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val (dotColor, labelText, containerBorder) = when (state) {
        HapticActuatorState.UNAVAILABLE -> Triple(
            CyberAmber,
            "HAPTICS UNAVAILABLE",
            CyberAmber.copy(alpha = 0.3f)
        )
        HapticActuatorState.OFF -> Triple(
            CyberTextMuted,
            "HAPTICS OFF",
            MaterialTheme.colorScheme.outline
        )
        HapticActuatorState.READY -> Triple(
            CyberGreen,
            "HAPTICS READY",
            CyberGreen.copy(alpha = 0.4f)
        )
        HapticActuatorState.PLAYING -> Triple(
            CyberCyan.copy(alpha = pulseAlpha),
            "ACTUATION ACTIVE",
            CyberCyan.copy(alpha = 0.8f)
        )
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, containerBorder),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator Dot
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = labelText,
                style = TechnicalMicroLabel,
                color = if (state == HapticActuatorState.PLAYING) CyberCyan else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
