package com.haptix.app.ui.screens.youtube

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haptix.app.domain.model.ProcessingStatus
import com.haptix.app.ui.components.HaptiXBackground
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.components.HaptiXProtocolEyebrow
import com.haptix.app.ui.components.motion.elasticOverscroll
import com.haptix.app.ui.theme.EditorialMetadataLabel
import com.haptix.app.ui.theme.HaptiXBodyLarge
import com.haptix.app.ui.theme.HaptiXShapeTokens
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.ObsidianBalthazarFontFamily
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.rememberUiHaptics

/**
 * Screen for ingesting a YouTube URL and generating platform-independent haptic representations.
 */
@Composable
fun YouTubeIngestionScreen(
    viewModel: YouTubeIngestionViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onLaunchPlayer: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = HaptiXThemeTokens.colors
    val uiHaptics = rememberUiHaptics()
    val scrollState = rememberScrollState()

    HaptiXBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .elasticOverscroll()
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            // Eyebrow and Header
            HaptiXProtocolEyebrow(text = "04 // AUTOMATED MULTIMODAL INGESTION")

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Analyze YouTube Video",
                fontFamily = ObsidianBalthazarFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Enter a public YouTube URL to synthesize synchronized tactile stimuli using the paper-inspired offline multimodal processing pipeline.",
                style = HaptiXBodyLarge,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // URL Input Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(HaptiXShapeTokens.card)
                    .background(colors.surface)
                    .border(1.dp, colors.borderSubtle, HaptiXShapeTokens.card)
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "YOUTUBE VIDEO TARGET",
                        style = TechnicalMicroLabel,
                        color = colors.accentPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = uiState.urlInput,
                        onValueChange = { viewModel.onUrlChanged(it) },
                        placeholder = {
                            Text(
                                text = "https://www.youtube.com/watch?v=...",
                                color = colors.textTertiary,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        enabled = !uiState.isProcessing,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accentPrimary,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.accentPrimary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (uiState.extractedVideoId != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Detected Video ID: ${uiState.extractedVideoId}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF7EE787)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HaptiXPrimaryButton(
                        text = if (uiState.isProcessing) "Synthesizing Haptics..." else "Analyze & Synthesize",
                        enabled = uiState.isValidUrl && !uiState.isProcessing,
                        onClick = {
                            uiHaptics.tap()
                            viewModel.submitUrl()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Error Banner
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF381010))
                        .border(1.dp, Color(0xFFF85149), RoundedCornerShape(8.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = Color(0xFFFFB4B4),
                        fontSize = 13.sp
                    )
                }
            }

            // Processing Pipeline Progression Card
            AnimatedVisibility(
                visible = uiState.isProcessing || uiState.isCompleted,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(HaptiXShapeTokens.card)
                            .background(colors.surface)
                            .border(1.dp, if (uiState.isCompleted) Color(0xFF7EE787) else colors.borderSubtle, HaptiXShapeTokens.card)
                            .padding(18.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MULTIMODAL SYNTHESIS PIPELINE",
                                    style = TechnicalMicroLabel,
                                    color = colors.accentPrimary
                                )
                                Text(
                                    text = "${(uiState.progress * 100).toInt()}%",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isCompleted) Color(0xFF7EE787) else colors.accentPrimary,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { uiState.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = if (uiState.isCompleted) Color(0xFF7EE787) else colors.accentPrimary,
                                trackColor = colors.controlSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = uiState.stageMessage,
                                style = EditorialMetadataLabel,
                                color = colors.textPrimary
                            )

                            // Pipeline stage chips
                            Spacer(modifier = Modifier.height(14.dp))
                            val stages = listOf(
                                "INGEST" to (uiState.progress >= 0.15f),
                                "VIDEO (RAFT)" to (uiState.progress >= 0.40f),
                                "AUDIO (LIBROSA)" to (uiState.progress >= 0.60f),
                                "FUSION" to (uiState.progress >= 0.75f),
                                "HAPTICS" to (uiState.progress >= 0.90f),
                                "READY" to uiState.isCompleted
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for ((name, active) in stages) {
                                    val bg = if (active) Color(0xFF1E3A2F) else colors.controlSurface
                                    val tc = if (active) Color(0xFF7EE787) else colors.textTertiary
                                    val bd = if (active) Color(0xFF238636) else colors.borderSubtle

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(bg)
                                            .border(1.dp, bd, RoundedCornerShape(4.dp))
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = tc,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            // Completion Action
                            if (uiState.isCompleted) {
                                Spacer(modifier = Modifier.height(18.dp))
                                val eventCount = uiState.generatedPattern?.eventCount ?: 0

                                Text(
                                    text = "Generated $eventCount validated haptic events for playback.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF7EE787)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                HaptiXPrimaryButton(
                                    text = "Play Video with Synchronized Haptics",
                                    onClick = {
                                        uiHaptics.tap()
                                        val activeId = uiState.activeJobId ?: "koji"
                                        onLaunchPlayer(activeId)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Navigation Back Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        uiHaptics.tap()
                        onNavigateBack()
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "← Return to Stimulus Library",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}
