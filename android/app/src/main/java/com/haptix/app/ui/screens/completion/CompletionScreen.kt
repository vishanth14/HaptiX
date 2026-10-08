package com.haptix.app.ui.screens.completion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.session.StudySessionState
import com.haptix.app.ui.components.HaptiXBackground
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.components.HaptiXProtocolEyebrow
import com.haptix.app.ui.components.HaptiXSecondaryButton
import com.haptix.app.ui.components.motion.breathe
import com.haptix.app.ui.components.motion.elasticOverscroll
import com.haptix.app.ui.theme.HaptiXBodyLarge
import com.haptix.app.ui.theme.HaptiXShapeTokens
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.LocalReducedMotion
import com.haptix.app.ui.theme.MotionTokens
import com.haptix.app.ui.theme.ObsidianBalthazarFontFamily
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.TechnicalReadoutValue
import com.haptix.app.ui.theme.rememberUiHaptics

/**
 * Screen 6: Protocol Complete / Research Session Conclusion.
 *
 * Calm signal checkmark animation with cyan pulse ring, Obsidian Balthazar headings,
 * structured research telemetry summary, and dual finish/re-evaluate CTAs.
 */
@Composable
fun CompletionScreen(
    sessionState: StudySessionState,
    onFinish: () -> Unit,
    onTestAnotherClip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiHaptics = rememberUiHaptics()
    val reducedMotion = LocalReducedMotion.current
    val scrollState = rememberScrollState()
    val colors = HaptiXThemeTokens.colors

    var settleCheckmark by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        settleCheckmark = true
        uiHaptics.confirm()
    }

    val checkmarkScale by animateFloatAsState(
        targetValue = if (settleCheckmark || reducedMotion) 1.0f else 0.82f,
        animationSpec = MotionTokens.Interactive,
        label = "checkmarkScale"
    )

    HaptiXBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .elasticOverscroll()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(36.dp))

                // Eyebrow Protocol Tag
                HaptiXProtocolEyebrow(
                    text = "04 // PROTOCOL COMPLETE",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Calm Signal Animation: Cyan outer ring + pulse + checkmark
                Box(
                    modifier = Modifier
                        .scale(checkmarkScale)
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .border(1.dp, colors.accentPrimary.copy(alpha = 0.5f), CircleShape)
                        .breathe(enabled = true, durationMs = 3200, minScale = 0.96f, maxScale = 1.04f),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(38.dp)) {
                        // Cyan outer signal accent ring
                        drawCircle(
                            color = colors.accentPrimary.copy(alpha = 0.25f),
                            radius = size.width / 2f,
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Checkmark
                        val strokeW = 2.5.dp.toPx()
                        drawLine(
                            color = colors.accentPrimary,
                            start = Offset(x = size.width * 0.24f, y = size.height * 0.54f),
                            end = Offset(x = size.width * 0.44f, y = size.height * 0.74f),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = colors.accentPrimary,
                            start = Offset(x = size.width * 0.44f, y = size.height * 0.74f),
                            end = Offset(x = size.width * 0.78f, y = size.height * 0.30f),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Study Segment Complete",
                    fontFamily = ObsidianBalthazarFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    letterSpacing = (-0.4).sp,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Audiovisual-tactile evaluations and performance telemetry successfully captured.",
                    style = HaptiXBodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Bento Research Summary
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Participant & Status Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(HaptiXShapeTokens.card)
                            .background(colors.surface)
                            .border(1.dp, colors.borderSubtle, HaptiXShapeTokens.card)
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PARTICIPANT RECORD",
                                    style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                                    color = colors.textTertiary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = sessionState.participant?.id ?: "ANONYMOUS",
                                    style = TechnicalReadoutValue.copy(fontSize = 14.sp),
                                    color = colors.accentPrimary
                                )
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(HaptiXShapeTokens.micro)
                                    .background(colors.focusSurface)
                                    .border(0.5.dp, colors.signalGreen.copy(alpha = 0.5f), HaptiXShapeTokens.micro)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "SESSION VERIFIED ✓",
                                    style = TechnicalMicroLabel.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                    color = colors.signalGreen
                                )
                            }
                        }
                    }

                    // 2x2 Bento Grid for metrics
                    val stimuliCount = sessionState.evaluatedVideoIds.size.coerceAtLeast(if (sessionState.selectedVideoId != null) 1 else 0)
                    val stimuliDetail = sessionState.selectedVideoId?.replace("_", " ")?.uppercase() ?: "EVALUATION SPECIMEN"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompletionBentoMetric(
                            label = "STIMULI EVALUATED",
                            value = if (stimuliCount < 10) "0$stimuliCount CLIPS" else "$stimuliCount CLIPS",
                            subtext = stimuliDetail,
                            modifier = Modifier.weight(1f)
                        )
                        CompletionBentoMetric(
                            label = "FEEDBACK LOGGED",
                            value = "${sessionState.feedbackResponses.size} METRICS",
                            subtext = "5-Axis completed",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CompletionBentoMetric(
                            label = "TACTILE CHANNEL",
                            value = if (sessionState.isHapticsEnabled) "SYNCHRONIZED" else "MUTED",
                            subtext = "Actuator verified",
                            modifier = Modifier.weight(1f)
                        )
                        CompletionBentoMetric(
                            label = "TELEMETRY LOG",
                            value = "${sessionState.performanceMetrics.size} SAMPLES",
                            subtext = "2000ms interval",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Bottom Progression CTAs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                HaptiXPrimaryButton(
                    text = "EVALUATE ANOTHER CLIP  →",
                    onClick = {
                        uiHaptics.confirm()
                        onTestAnotherClip()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                HaptiXSecondaryButton(
                    text = "Finish Study Session",
                    onClick = {
                        uiHaptics.tap()
                        onFinish()
                    }
                )
            }
        }
    }
}

@Composable
private fun CompletionBentoMetric(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    val colors = HaptiXThemeTokens.colors
    Box(
        modifier = modifier
            .clip(HaptiXShapeTokens.card)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, HaptiXShapeTokens.card)
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = label,
                style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                color = colors.textTertiary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = TechnicalReadoutValue.copy(fontSize = 14.sp),
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                color = colors.textSecondary
            )
        }
    }
}

