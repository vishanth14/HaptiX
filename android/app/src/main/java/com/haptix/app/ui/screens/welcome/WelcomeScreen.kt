package com.haptix.app.ui.screens.welcome

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.ui.components.HaptiXBackground
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.components.HaptiXProtocolEyebrow
import com.haptix.app.ui.components.HaptiXWaveform
import com.haptix.app.ui.components.motion.elasticOverscroll
import com.haptix.app.ui.theme.HaptiXBodyLarge
import com.haptix.app.ui.theme.HaptiXShapeTokens
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.ObsidianBalthazarFontFamily
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.TechnicalReadoutValue

/**
 * Screen 1: Welcome / Research Intake Landing.
 *
 * Establishes HaptiX identity:
 * Obsidian Dark, Balthazar display typography, interactive multi-trace research waveform,
 * technical parameter readouts, and precision illuminated CTA button.
 */
@Composable
fun WelcomeScreen(
    onBeginStudy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val colors = HaptiXThemeTokens.colors

    HaptiXBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .elasticOverscroll()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(32.dp))

                // Eyebrow Protocol Tag
                HaptiXProtocolEyebrow(text = "HAPTIX // RESEARCH PROTOCOL")

                Spacer(modifier = Modifier.height(12.dp))

                // Brand Display Heading
                Text(
                    text = "HaptiX",
                    fontFamily = ObsidianBalthazarFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 46.sp,
                    letterSpacing = (-0.8).sp,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "MULTIMODAL HAPTIC RESEARCH",
                    style = TechnicalMicroLabel.copy(
                        fontSize = 11.sp,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.accentPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "A controlled research study exploring synchronized visual, auditory, and frequency-calibrated tactile stimuli.",
                    style = HaptiXBodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Enclosed Multimodal Waveform Visualizer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(HaptiXShapeTokens.card)
                        .background(colors.surface)
                        .border(1.dp, colors.borderSubtle, HaptiXShapeTokens.card)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MULTIMODAL SYNCHRONIZATION TRACE",
                                style = TechnicalMicroLabel,
                                color = colors.textTertiary
                            )
                            Text(
                                text = "LIVE // 150-250 Hz",
                                style = TechnicalMicroLabel,
                                color = colors.accentPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        HaptiXWaveform(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Study Parameters Section - Bento Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STUDY INSTRUMENT SPECIFICATIONS",
                        style = TechnicalMicroLabel,
                        color = colors.accentPrimary
                    )
                    Text(
                        text = "PROTOCOL READY",
                        style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                        color = colors.signalGreen
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2x2 Bento Grid Layout for Study Parameters
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoParameterCell(
                            label = "VIDEO CHANNEL",
                            primaryValue = "MULTIMODAL",
                            subtext = "Calibrated stimulus video",
                            modifier = Modifier.weight(1f)
                        )
                        BentoParameterCell(
                            label = "AUDIO CHANNEL",
                            primaryValue = "STEREO SYNC",
                            subtext = "Calibrated reference audio",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoParameterCell(
                            label = "TACTILE CHANNEL",
                            primaryValue = "ACTUATION",
                            subtext = "Hardware-timed patterns",
                            modifier = Modifier.weight(1f)
                        )
                        BentoParameterCell(
                            label = "MEASUREMENT",
                            primaryValue = "5-AXIS LIKERT",
                            subtext = "Perceptual evaluation",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Primary Bottom Action CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
            ) {
                HaptiXPrimaryButton(
                    text = "BEGIN STUDY  →",
                    onClick = onBeginStudy
                )
            }
        }
    }
}

@Composable
private fun BentoParameterCell(
    label: String,
    primaryValue: String,
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
                text = primaryValue,
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
