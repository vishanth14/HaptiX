package com.haptix.app.ui.screens.welcome

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.ui.components.CyberBackground
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.theme.CyberCyan
import com.haptix.app.ui.theme.CyberGreen
import com.haptix.app.ui.theme.CyberViolet
import com.haptix.app.ui.theme.LocalSpacing
import com.haptix.app.ui.theme.TechnicalMicroLabel

/**
 * Redesigned Welcome Screen with editorial product-style composition,
 * abstract multimodal hero visualization, and substantial primary CTA.
 */
@Composable
fun WelcomeScreen(
    onBeginStudy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        CyberBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.xl, vertical = spacing.lg)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(spacing.sm))

                    // Small Eyebrow
                    Text(
                        text = "RESEARCH PROTOCOL // EXP-2026",
                        style = TechnicalMicroLabel.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(spacing.xs))

                    // Large Editorial Title
                    Text(
                        text = "HaptiX",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Multimodal Haptic-Video Evaluation",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(spacing.lg))

                    // Large Visual / Abstract Multimodal Hero Area (24dp rounded)
                    MultimodalHeroVisual(modifier = Modifier.fillMaxWidth().height(160.dp))

                    Spacer(modifier = Modifier.height(spacing.lg))

                    // Compact Study Information Container (20dp rounded)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
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
                                    text = "STUDY SPECIFICATION",
                                    style = TechnicalMicroLabel,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "SECURE PROTOCOL",
                                    style = TechnicalMicroLabel,
                                    color = CyberGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(spacing.xs))

                            Text(
                                text = "Controlled experimental evaluation of human perception, tactile realism, and multimodal synchrony during audio-visual playback.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(spacing.md))

                            // Steps overview
                            val steps = listOf(
                                "01" to "Participant intake",
                                "02" to "Stimulus library",
                                "03" to "Audio-tactile playback",
                                "04" to "Evaluation questionnaire"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                steps.forEach { (num, label) ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(6.dp)) {
                                            Text(
                                                text = num,
                                                style = TechnicalMicroLabel.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.xl))
                }

                // Primary CTA & Bottom Readiness
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HaptiXPrimaryButton(
                        text = "BEGIN STUDY  →",
                        onClick = onBeginStudy
                    )

                    Spacer(modifier = Modifier.height(spacing.md))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(CyberGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "INTERFACE READY",
                            style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.xs))
                }
            }
        }
    }
}

/**
 * Large abstract visual hero communicating VIDEO + AUDIO + HAPTICS
 * with subtle animated frequency waves and layered glowing curves.
 */
@Composable
private fun MultimodalHeroVisual(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HeroWaves")
    val phaseOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        color = Color(0xFF0D1320),
        modifier = modifier.clip(RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Radial atmospheric depth behind waveform
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CyberCyan.copy(alpha = 0.15f),
                            CyberViolet.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.55f
                    ),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.55f
                )

                // Cyan Wave (Audio-Video Stream)
                val path1 = Path()
                path1.moveTo(0f, h * 0.5f)
                val waveLen = w / 2.2f
                for (x in 0..w.toInt() step 5) {
                    val y = h * 0.5f + (kotlin.math.sin((x / waveLen) * 2 * Math.PI + phaseOffset) * 24f).toFloat()
                    path1.lineTo(x.toFloat(), y)
                }
                drawPath(
                    path = path1,
                    color = CyberCyan.copy(alpha = 0.75f),
                    style = Stroke(width = 2.2.dp.toPx())
                )

                // Violet Wave (Tactile/Haptic Frequency)
                val path2 = Path()
                path2.moveTo(0f, h * 0.5f)
                for (x in 0..w.toInt() step 5) {
                    val y = h * 0.5f + (kotlin.math.cos((x / (waveLen * 0.8f)) * 2 * Math.PI - phaseOffset) * 20f).toFloat()
                    path2.lineTo(x.toFloat(), y)
                }
                drawPath(
                    path = path2,
                    color = CyberViolet.copy(alpha = 0.6f),
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }

            // Central Floating Pill Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "VIDEO  +  AUDIO  +  HAPTICS",
                    style = TechnicalMicroLabel.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = CyberCyan
                )
            }
        }
    }
}
