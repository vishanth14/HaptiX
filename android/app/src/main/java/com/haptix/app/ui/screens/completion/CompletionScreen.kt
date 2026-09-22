package com.haptix.app.ui.screens.completion

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.session.StudySessionState
import com.haptix.app.ui.components.CyberBackground
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.components.HaptiXSecondaryButton
import com.haptix.app.ui.theme.CyberCyan
import com.haptix.app.ui.theme.CyberGreen
import com.haptix.app.ui.theme.LocalSpacing
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.TechnicalValueLabel

/**
 * Screen 6: Study Session Completion.
 * Calm, premium scientific conclusion screen with subtle animated cyan pulse ring,
 * 22dp summary card, and academic conclusion actions.
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
        CyberBackground(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.xl),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(spacing.md))

                // Main Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Large Success Checkmark with subtle cyan pulse ring
                    SubtleSuccessCheckmark()

                    Spacer(modifier = Modifier.height(spacing.lg))

                    Text(
                        text = "SESSION COMPLETE",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(spacing.xs))

                    Text(
                        text = "Your experimental session has been captured successfully.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(spacing.xl))

                    // Clean Session Summary Card (22dp rounded)
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
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
                                    text = "SESSION SUMMARY",
                                    style = TechnicalMicroLabel,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "DATA RECORDED",
                                    style = TechnicalMicroLabel,
                                    color = CyberGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(spacing.md))

                            SummaryRow(
                                label = "PARTICIPANT",
                                value = sessionState.participant?.id ?: "ANONYMOUS"
                            )
                            SummaryRow(
                                label = "STIMULUS",
                                value = sessionState.selectedVideoId?.uppercase() ?: "NONE"
                            )
                            SummaryRow(
                                label = "HAPTICS",
                                value = if (sessionState.isHapticsEnabled) "ENABLED" else "MUTED"
                            )
                            SummaryRow(
                                label = "RESPONSES",
                                value = "${sessionState.feedbackResponses.size} / 05"
                            )
                        }
                    }
                }

                // Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HaptiXPrimaryButton(
                        text = "FINISH STUDY",
                        onClick = onFinish
                    )

                    Spacer(modifier = Modifier.height(spacing.sm))

                    HaptiXSecondaryButton(
                        text = "Evaluate Another Video Clip",
                        onClick = onTestAnotherClip
                    )
                }
            }
        }
    }
}

/**
 * Clean scientific checkmark icon with a restrained cyan ambient pulse ring.
 */
@Composable
private fun SubtleSuccessCheckmark(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CheckmarkPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        // Subtle animated outer cyan ring
        Canvas(modifier = Modifier.size(68.dp).scale(pulseScale)) {
            drawCircle(
                color = CyberCyan.copy(alpha = pulseAlpha),
                style = Stroke(width = 1.8.dp.toPx())
            )
        }

        // Inner solid circular badge with checkmark
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val strokeWidth = 2.5.dp.toPx()
                val pathColor = CyberCyan
                // Checkmark lines
                drawLine(
                    color = pathColor,
                    start = Offset(x = size.width * 0.2f, y = size.height * 0.52f),
                    end = Offset(x = size.width * 0.44f, y = size.height * 0.76f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = pathColor,
                    start = Offset(x = size.width * 0.44f, y = size.height * 0.76f),
                    end = Offset(x = size.width * 0.82f, y = size.height * 0.28f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TechnicalMicroLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = TechnicalValueLabel.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
