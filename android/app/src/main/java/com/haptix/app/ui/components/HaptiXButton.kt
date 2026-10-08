package com.haptix.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.haptix.app.ui.components.motion.bounceClick
import com.haptix.app.ui.theme.HaptiXButtonText
import com.haptix.app.ui.theme.HaptiXShapeTokens
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.rememberUiHaptics

/**
 * Precision Cyberpunk Research Action Button.
 *
 * Primary:
 * Dark obsidian body with illuminated cyan perimeter and subtle top highlight.
 *
 * Secondary:
 * Dark translucent tonal surface with hairline border.
 */
@Composable
fun HaptiXButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true
) {
    val uiHaptics = rememberUiHaptics()
    val colors = HaptiXThemeTokens.colors
    val shape = HaptiXShapeTokens.button

    val backgroundColor = if (isPrimary) {
        if (enabled) colors.elevated else colors.elevated.copy(alpha = 0.4f)
    } else {
        colors.controlSurface
    }

    val contentColor = if (isPrimary) {
        if (enabled) colors.accentPrimary else colors.textTertiary
    } else {
        if (enabled) colors.textPrimary else colors.textTertiary
    }

    val border = if (isPrimary) {
        BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    colors.accentPrimary.copy(alpha = 0.8f),
                    colors.accentSecondary.copy(alpha = 0.5f),
                    colors.accentPrimary.copy(alpha = 0.8f)
                )
            )
        )
    } else {
        BorderStroke(0.5.dp, colors.borderSubtle)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(border, shape)
            .bounceClick(
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    uiHaptics.tap()
                    onClick()
                }
            )
            .semantics { this.role = Role.Button }
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = text,
            style = HaptiXButtonText,
            color = contentColor
        )
    }
}

@Composable
fun HaptiXPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true
) {
    HaptiXButton(
        text = text,
        onClick = onClick,
        modifier = if (fullWidth) modifier.fillMaxWidth() else modifier,
        enabled = enabled,
        isPrimary = true
    )
}

/**
 * Secondary Action Button for HaptiX Research workflows.
 */
@Composable
fun HaptiXSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true
) {
    HaptiXButton(
        text = text,
        onClick = onClick,
        modifier = if (fullWidth) modifier.fillMaxWidth() else modifier,
        enabled = enabled,
        isPrimary = false
    )
}

/**
 * Backward compatibility alias for CyberButton.
 */
@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true
) {
    HaptiXButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isPrimary = isPrimary
    )
}
