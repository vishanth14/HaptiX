package com.haptix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.ui.theme.EditorialMetadataLabel
import com.haptix.app.ui.theme.LocalSpacing

/**
 * Clean Apple-style questionnaire item for experimental evaluation.
 * Sits directly on the background with generous whitespace and a subtle hairline separator.
 */
@Composable
fun FeedbackQuestionCard(
    questionNumber: Int,
    questionText: String,
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lowAnchorLabel: String = "1 — Strongly Disagree",
    highAnchorLabel: String = "5 — Strongly Agree"
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Question number indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QUESTION $questionNumber OF 05",
                style = EditorialMetadataLabel.copy(
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )

            if (rating > 0) {
                Text(
                    text = "$rating / 5",
                    style = EditorialMetadataLabel.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Question text in clear Apple typography
        Text(
            text = questionText,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                lineHeight = 24.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Large 5-star rating control (48dp touch targets)
        StarRating(
            rating = rating,
            onRatingChanged = onRatingChanged,
            modifier = Modifier.fillMaxWidth(),
            starSize = 34.dp,
            touchTargetSize = 52.dp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Anchor labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
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

        Spacer(modifier = Modifier.height(18.dp))

        // Subtle hairline separator between questions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        )
    }
}

