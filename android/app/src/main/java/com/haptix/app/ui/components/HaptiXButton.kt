package com.haptix.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.ui.theme.CyberCyan

/**
 * Standard Primary button for key progression actions in the research flow (e.g. "Begin Study", "Continue").
 * Elevated with generous 16dp rounded geometry, subtle press scale feedback, and restrained luminous accents.
 */
@Composable
fun HaptiXPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(150),
        label = "btnScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        border = if (enabled) BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 50.dp)
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 0.6.sp,
                fontWeight = FontWeight.SemiBold
            ),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Secondary button for alternative actions (e.g. "Back", "Skip", "Retry").
 */
@Composable
fun HaptiXSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(150),
        label = "btnSecScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 50.dp)
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 0.3.sp,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Clean outlined button for secondary options with subtle technical border.
 */
@Composable
fun HaptiXOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(150),
        label = "btnOutScale"
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 50.dp)
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 0.3.sp,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
    }
}
