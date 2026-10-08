package com.haptix.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.haptix.app.ui.components.motion.breathe
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.MotionTokens
import com.haptix.app.ui.theme.TechnicalMicroLabel

/**
 * Status beacon for physical hardware and instrument states.
 */
@Composable
fun HaptiXSignalBeacon(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = HaptiXThemeTokens.colors.signalGreen,
    inactiveColor: Color = HaptiXThemeTokens.colors.textTertiary,
    breatheWhenActive: Boolean = true
) {
    val targetColor = if (isActive) activeColor else inactiveColor
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = MotionTokens.colorSpring(),
        label = "beaconColor"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .breathe(enabled = isActive && breatheWhenActive, durationMs = 2400)
                .background(animatedColor)
        )

        Text(
            text = label.uppercase(),
            style = TechnicalMicroLabel,
            color = if (isActive) HaptiXThemeTokens.colors.textPrimary else HaptiXThemeTokens.colors.textTertiary
        )
    }
}
