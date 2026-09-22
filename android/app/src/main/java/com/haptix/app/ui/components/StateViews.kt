package com.haptix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.ui.theme.AppleSystemRedDark
import com.haptix.app.ui.theme.LocalSpacing

/**
 * Clean centered research loading state indicator.
 */
@Composable
fun HaptiXLoadingIndicator(
    modifier: Modifier = Modifier,
    message: String = "Loading stimuli..."
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier.padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(spacing.md))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Calm research error notification banner with rounded Apple-style container.
 */
@Composable
fun HaptiXErrorMessage(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    val spacing = LocalSpacing.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Notice",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = AppleSystemRedDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(14.dp))
                HaptiXSecondaryButton(
                    text = "Retry",
                    onClick = onRetry,
                    fullWidth = false
                )
            }
        }
    }
}

/**
 * Calm, clear empty state notification when stimuli or data are not found.
 */
@Composable
fun HaptiXEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier.padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(spacing.lg))
            HaptiXSecondaryButton(
                text = actionLabel,
                onClick = onAction,
                fullWidth = false
            )
        }
    }
}

