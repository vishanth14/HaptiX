package com.haptix.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haptix.app.ui.theme.CyberCyan
import com.haptix.app.ui.theme.LocalSpacing
import com.haptix.app.ui.theme.TechnicalMicroLabel

/**
 * Standardized questionnaire item card for feedback evaluation with generous 22dp rounded geometry.
 * Styled as a clean, modern experimental evaluation module.
 */
@Composable
fun FeedbackQuestionCard(
    questionNumber: Int,
    questionText: String,
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lowAnchorLabel: String = "1 — Low",
    highAnchorLabel: String = "5 — High"
) {
    val spacing = LocalSpacing.current

    val borderColor by animateColorAsState(
        targetValue = if (rating > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "qBorder"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, borderColor),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUESTION 0$questionNumber / 05",
                    style = TechnicalMicroLabel,
                    color = MaterialTheme.colorScheme.primary
                )

                if (rating > 0) {
                    Text(
                        text = "RATING: $rating/5",
                        style = TechnicalMicroLabel,
                        color = CyberCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            Text(
                text = questionText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(spacing.md))

            // Centered Star Rating with Radiant Feedback
            StarRating(
                rating = rating,
                onRatingChanged = onRatingChanged,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(spacing.xs))

            // Technical Rating anchors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lowAnchorLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = highAnchorLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
