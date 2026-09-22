package com.haptix.app.ui.screens.completion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.session.StudySessionState
import com.haptix.app.ui.components.HaptiXBackground
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.components.HaptiXSecondaryButton
import com.haptix.app.ui.theme.EditorialMetadataLabel
import com.haptix.app.ui.theme.LocalSpacing

/**
 * Screen 6: Study Session Completion.
 * Calm, professional, Apple-inspired conclusion screen.
 * Restrained typography, elegant checkmark, clear research summary, and minimal actions.
 */
@Composable
fun CompletionScreen(
    sessionState: StudySessionState,
    onFinish: () -> Unit,
    onTestAnotherClip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        HaptiXBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.xl),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(spacing.md))

                // Main Calm Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Minimal Elegant Checkmark
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(32.dp)) {
                            val strokeWidth = 3.dp.toPx()
                            val checkColor = Color(0xFF30D158) // Apple Success Green
                            drawLine(
                                color = checkColor,
                                start = Offset(x = size.width * 0.22f, y = size.height * 0.54f),
                                end = Offset(x = size.width * 0.44f, y = size.height * 0.76f),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = checkColor,
                                start = Offset(x = size.width * 0.44f, y = size.height * 0.76f),
                                end = Offset(x = size.width * 0.80f, y = size.height * 0.28f),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.lg))

                    Text(
                        text = "Study Complete",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Thank you.\nYour responses have been recorded.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(spacing.largeSectionGap))

                    // Research Metrics Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "STUDY OVERVIEW",
                                style = EditorialMetadataLabel.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.0.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            CompletionSummaryRow(
                                label = "Participant",
                                value = sessionState.participant?.id ?: "Anonymous"
                            )
                            CompletionSummaryRow(
                                label = "Stimulus evaluated",
                                value = sessionState.selectedVideoId?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "None"
                            )
                            CompletionSummaryRow(
                                label = "Haptic condition",
                                value = if (sessionState.isHapticsEnabled) "Active actuation" else "Muted (Control)"
                            )
                            CompletionSummaryRow(
                                label = "Evaluations submitted",
                                value = "${sessionState.feedbackResponses.size} responses"
                            )
                        }
                    }
                }

                // Bottom Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HaptiXPrimaryButton(
                        text = "FINISH",
                        onClick = onFinish
                    )

                    Spacer(modifier = Modifier.height(spacing.sm))

                    HaptiXSecondaryButton(
                        text = "Evaluate Another Stimulus",
                        onClick = onTestAnotherClip
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionSummaryRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = EditorialMetadataLabel.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
