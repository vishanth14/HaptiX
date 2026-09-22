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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Standard Apple-style Primary filled button for key research progression actions (e.g. "BEGIN STUDY →").
 * Features 18dp rounded geometry, solid system accent container, and subtle 0.98 press scale.
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
        animationSpec = tween(120),
        label = "btnScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        border = null,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 15.dp),
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 52.dp)
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Secondary button for alternative actions (e.g. "Back", "Skip", "Finish Later").
 * Sits gently on secondary surface without loud borders.
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
        animationSpec = tween(120),
        label = "btnSecScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        border = null,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 52.dp)
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.1.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Clean outlined button for secondary choices with subtle Apple-style hairline border.
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
        animationSpec = tween(120),
        label = "btnOutScale"
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = 52.dp)
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.1.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

