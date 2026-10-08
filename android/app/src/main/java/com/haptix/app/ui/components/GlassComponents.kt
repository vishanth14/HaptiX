package com.haptix.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.ui.components.motion.bounceClick
import com.haptix.app.ui.theme.MaterialLevel
import com.haptix.app.ui.theme.MaterialTokens
import com.haptix.app.ui.theme.rememberUiHaptics

/**
 * Fundamental translucent material surface container.
 * Combines layered tonal surface opacity, hairline border (0.5dp), and soft clipping.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MaterialTokens.MediumShapeRadius),
    level: MaterialLevel = MaterialLevel.Regular,
    border: BorderStroke? = BorderStroke(MaterialTokens.HairlineBorderWidth, MaterialTokens.hairlineBorderColor()),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTokens.surfaceColor(level),
        border = border,
        content = content
    )
}

/**
 * Translucent material card for content grouping.
 * Supports optional spring bounce on click.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MaterialTokens.LargeShapeRadius),
    level: MaterialLevel = MaterialLevel.Regular,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.bounceClick(role = Role.Button, onClick = onClick)
    } else {
        modifier
    }

    GlassSurface(
        modifier = cardModifier,
        shape = shape,
        level = level
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            content = content
        )
    }
}

/**
 * Translucent top or floating navigation toolbar with hairline divider.
 */
@Composable
fun GlassToolbar(
    modifier: Modifier = Modifier,
    level: MaterialLevel = MaterialLevel.Chrome,
    content: @Composable RowScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTokens.surfaceColor(level))
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
        // Hairline bottom divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTokens.HairlineBorderWidth)
                .background(MaterialTokens.hairlineBorderColor())
        )
    }
}

/**
 * Translucent circular or rounded control container (e.g. for media buttons, badges).
 */
@Composable
fun GlassControl(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    level: MaterialLevel = MaterialLevel.Thin,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val controlModifier = if (onClick != null) {
        modifier.bounceClick(role = Role.Button, onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = controlModifier,
        shape = shape,
        color = MaterialTokens.surfaceColor(level),
        border = BorderStroke(MaterialTokens.HairlineBorderWidth, MaterialTokens.hairlineBorderColor()),
        content = content
    )
}

/**
 * Apple-inspired Material Button with tactile spring bounce and native UI haptics.
 */
@Composable
fun MaterialButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true
) {
    val uiHaptics = rememberUiHaptics()
    val shape = RoundedCornerShape(MaterialTokens.PillShapeRadius)

    val backgroundColor = if (isPrimary) {
        if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    } else {
        MaterialTokens.surfaceColor(MaterialLevel.Thin)
    }

    val contentColor = if (isPrimary) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val border = if (!isPrimary) {
        BorderStroke(MaterialTokens.HairlineBorderWidth, MaterialTokens.hairlineBorderColor())
    } else {
        null
    }

    Surface(
        shape = shape,
        color = backgroundColor,
        border = border,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .bounceClick(
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    uiHaptics.tap()
                    onClick()
                }
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = contentColor
            )
        }
    }
}

/**
 * Apple-inspired Material Pill selection chip with spring bounce and selection state.
 */
@Composable
fun MaterialPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiHaptics = rememberUiHaptics()
    val shape = RoundedCornerShape(MaterialTokens.SmallShapeRadius)

    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTokens.surfaceColor(MaterialLevel.UltraThin)
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val border = BorderStroke(
        width = MaterialTokens.HairlineBorderWidth,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTokens.hairlineBorderColor()
    )

    Surface(
        shape = shape,
        color = backgroundColor,
        border = border,
        modifier = modifier
            .defaultMinSize(minWidth = 72.dp, minHeight = 48.dp)
            .clip(shape)
            .semantics {
                this.role = Role.RadioButton
                this.selected = isSelected
            }
            .bounceClick(
                role = Role.RadioButton,
                onClick = {
                    uiHaptics.selection()
                    onClick()
                }
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = contentColor
            )
        }
    }
}

/**
 * Grouped iOS settings-style sheet container for form fields and research parameters.
 */
@Composable
fun MaterialSheet(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MaterialTokens.LargeShapeRadius),
    level: MaterialLevel = MaterialLevel.Regular,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        level = level
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            content = content
        )
    }
}
