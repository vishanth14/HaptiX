package com.haptix.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.ui.theme.EditorialMetadataLabel

/**
 * Editorial Apple-style section header for study screens.
 * Uses clean system typography, restrained letter-spacing, and clear hierarchy.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    stepLabel: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (stepLabel != null) {
            Text(
                text = stepLabel.uppercase(),
                style = EditorialMetadataLabel.copy(
                    letterSpacing = 1.0.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

