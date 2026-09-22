package com.haptix.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import com.haptix.app.ui.theme.CyberThemeMode
import com.haptix.app.ui.theme.LocalCyberThemeMode
import com.haptix.app.ui.theme.LocalSpacing

/**
 * Minimal Apple-inspired top navigation bar for HaptiX research application.
 * Features generous spacing, clean editorial typography, and an unobtrusive theme switch.
 */
@Composable
fun HaptiXTopBar(
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "HaptiX",
    subtitle: String = "Research Study"
) {
    val spacing = LocalSpacing.current
    val currentTheme = LocalCyberThemeMode.current
    val isDark = currentTheme == CyberThemeMode.DARK

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenHorizontal, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Editorial Brand & Context
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Unobtrusive, minimal theme toggle button (48dp touch target)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 24.dp),
                        role = Role.Switch,
                        onClick = onToggleTheme
                    )
                    .semantics {
                        this.role = Role.Switch
                        this.contentDescription = "Switch to ${if (isDark) "Light" else "Dark"} Mode"
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = if (isDark) "◐" else "◑",
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Extremely subtle hairline separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        )
    }
}

/**
 * Backward compatibility alias for CyberTopBar.
 */
@Composable
fun CyberTopBar(
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    HaptiXTopBar(
        onToggleTheme = onToggleTheme,
        modifier = modifier
    )
}

