package com.haptix.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.haptix.app.ui.theme.CyberCyan
import com.haptix.app.ui.theme.CyberGreen
import com.haptix.app.ui.theme.CyberThemeMode
import com.haptix.app.ui.theme.LocalCyberThemeMode
import com.haptix.app.ui.theme.LocalSpacing
import com.haptix.app.ui.theme.TechnicalMicroLabel

/**
 * Clean, compact top bar providing laboratory status and a sleek segmented theme switcher.
 */
@Composable
fun CyberTopBar(
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val currentTheme = LocalCyberThemeMode.current
    val isDark = currentTheme == CyberThemeMode.DARK

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand & Status Block
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "HaptiX",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.width(spacing.sm))

                // Small vertical divider
                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )

                Spacer(modifier = Modifier.width(spacing.sm))

                // System status indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CyberGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "READY",
                        style = TechnicalMicroLabel.copy(fontSize = 9.sp),
                        color = CyberGreen
                    )
                }
            }

            // Compact Floating Segmented Pill Theme Switcher
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        role = Role.Switch,
                        onClick = onToggleTheme
                    )
                    .semantics {
                        this.role = Role.Switch
                        this.contentDescription = "Switch to ${if (isDark) "Light" else "Dark"} Theme"
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDark) "☾  DARK" else "☀  LIGHT",
                        style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Extremely subtle separator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        )
    }
}
