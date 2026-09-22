package com.haptix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.haptix.app.ui.theme.CyberCyan
import com.haptix.app.ui.theme.CyberRed
import com.haptix.app.ui.theme.CyberRedContainer
import com.haptix.app.ui.theme.LocalSpacing
import com.haptix.app.ui.theme.TechnicalMicroLabel

/**
 * Clean centered research loading state indicator with laboratory micro-labels.
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
            strokeWidth = 2.5.dp
        )
        Spacer(modifier = Modifier.height(spacing.md))
        Text(
            text = message.uppercase(),
            style = TechnicalMicroLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Calm research error notification banner with laboratory system notice badge.
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
            .clip(RoundedCornerShape(6.dp))
            .background(CyberRedContainer)
            .border(1.dp, CyberRed.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(spacing.md)
    ) {
        Column {
            Text(
                text = "[ SYSTEM NOTICE // ERROR ]",
                style = TechnicalMicroLabel,
                color = CyberRed
            )
            Spacer(modifier = Modifier.height(spacing.xxs))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = CyberRed
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(spacing.sm))
                HaptiXOutlinedButton(
                    text = "Retry",
                    onClick = onRetry
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
            text = "[ ${title.uppercase()} ]",
            style = TechnicalMicroLabel,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
