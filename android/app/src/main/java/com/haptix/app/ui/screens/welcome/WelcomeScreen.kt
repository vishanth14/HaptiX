package com.haptix.app.ui.screens.welcome

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.ui.components.HaptiXBackground
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.theme.AppleSystemBlueDark
import com.haptix.app.ui.theme.EditorialMetadataLabel
import com.haptix.app.ui.theme.LocalSpacing

/**
 * Apple Music inspired editorial welcome landing screen for HaptiX.
 * Features large typography, a rich media hero visual composition, study overview metrics,
 * and a prominent rounded CTA button.
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
        HaptiXBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.screenHorizontal)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(28.dp))

                    // Small category tag
                    Text(
                        text = "RESEARCH STUDY",
                        style = EditorialMetadataLabel.copy(
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Large Editorial Title Hierarchy
                    Text(
                        text = "HaptiX",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.6).sp,
                            lineHeight = 44.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Multimodal Haptic\nResearch Study",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 34.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Investigating the perceptual synchrony, tactile fidelity, and subjective realism of frequency-matched multimodal stimuli.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Large Apple Music-style Editorial Media Hero Composition (24dp rounded)
                    AppleEditorialHeroVisual(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Study Overview Section (sitting directly on background or subtle grouping)
                    Text(
                        text = "Study Overview",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            StudyOverviewRow(label = "Video stimuli", value = "03")

                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            StudyOverviewRow(label = "Haptic conditions", value = "06")

                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            StudyOverviewRow(label = "Feedback", value = "05 questions")
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))
                }

                // Bottom Call To Action: Apple-style filled rounded button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    HaptiXPrimaryButton(
                        text = "BEGIN STUDY  →",
                        onClick = onBeginStudy
                    )
                }
            }
        }
    }
}

/**
 * Editorial Hero Visual Composition:
 * Organic waveform and tactile pulse curves inside a rounded media canvas with subtle depth.
 */
@Composable
private fun AppleEditorialHeroVisual(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF151518))
    ) {
        // Generative waveform artwork
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Subtle dark background gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141F30),
                        Color(0xFF0A0D14)
                    )
                )
            )

            // Tactile frequency waveform 1
            val path1 = Path()
            path1.moveTo(0f, h * 0.52f)
            val step = 6
            for (x in 0..w.toInt() step step) {
                val factor = (x / w) * Math.PI * 3.2
                val y = h * 0.52f + (kotlin.math.sin(factor) * (h * 0.28f)).toFloat()
                path1.lineTo(x.toFloat(), y)
            }
            drawPath(
                path = path1,
                color = AppleSystemBlueDark.copy(alpha = 0.65f),
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Audio waveform 2
            val path2 = Path()
            path2.moveTo(0f, h * 0.48f)
            for (x in 0..w.toInt() step step) {
                val factor = (x / w) * Math.PI * 4.8
                val y = h * 0.48f + (kotlin.math.cos(factor) * (h * 0.20f)).toFloat()
                path2.lineTo(x.toFloat(), y)
            }
            drawPath(
                path = path2,
                color = Color.White.copy(alpha = 0.35f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Overlay scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )

        // Bottom badge pills inside hero
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Haptic frequency", "Video", "Audio", "Research").forEach { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = tag,
                        style = EditorialMetadataLabel.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyOverviewRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

