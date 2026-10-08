package com.haptix.app.ui.screens.video

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import com.haptix.app.ui.theme.HaptiXShapeTokens
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haptix.app.ui.components.HaptiXBackground
import com.haptix.app.ui.components.HaptiXEmptyState
import com.haptix.app.ui.components.HaptiXErrorMessage
import com.haptix.app.ui.components.HaptiXFeaturedStimulusCard
import com.haptix.app.ui.components.HaptiXInput
import com.haptix.app.ui.components.HaptiXLoadingIndicator
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.components.HaptiXProtocolEyebrow
import com.haptix.app.ui.components.HaptiXStimulusSpecimenCard
import com.haptix.app.ui.components.motion.elasticOverscroll
import com.haptix.app.ui.theme.HaptiXBodyLarge
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.ObsidianBalthazarFontFamily
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.rememberUiHaptics

/**
 * Screen 3: Stimulus Library / Research Specimen Repository.
 *
 * Media-first layout presenting calibrated audiovisual and tactile stimuli specimens.
 * Displays Featured specimen with 16:9 dominant viewport, haptic profile metadata,
 * and specimen cards with lensing depth and elastic overscroll.
 */
@Composable
fun VideoListScreen(
    viewModel: VideoListViewModel = viewModel(),
    evaluatedVideoIds: List<String> = emptyList(),
    onVideoSelected: (String) -> Unit = {},
    onNavigateToYouTube: () -> Unit = {},
    onNavigateToCalibration: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = HaptiXThemeTokens.colors

    HaptiXBackground(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    HaptiXLoadingIndicator(message = "Loading experimental stimuli...")
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HaptiXErrorMessage(
                        message = uiState.errorMessage ?: "Unknown error loading stimuli.",
                        onRetry = { viewModel.loadVideos() }
                    )
                }
            }

            uiState.videos.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HaptiXEmptyState(
                        title = "No Stimuli Available",
                        description = "No experimental video items are currently configured for this session.",
                        actionLabel = "Reload",
                        onAction = { viewModel.loadVideos() }
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .elasticOverscroll(),
                    contentPadding = PaddingValues(
                        start = 22.dp,
                        end = 22.dp,
                        top = 16.dp,
                        bottom = 40.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Section 1: Editorial Header
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(16.dp))

                            HaptiXProtocolEyebrow(text = "HAPTIX // MULTIMODAL HAPTIC RESEARCH")

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Stimulus Library",
                                fontFamily = ObsidianBalthazarFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 34.sp,
                                letterSpacing = (-0.4).sp,
                                color = colors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Select an experimental specimen to evaluate multimodal synchronization.",
                                style = HaptiXBodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                                color = colors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Content Area A: Primary / Next Evaluation Target
                    val nextTargetVideo = uiState.videos.firstOrNull { it.id !in evaluatedVideoIds }
                    val featuredVideo = nextTargetVideo ?: uiState.videos.firstOrNull()
                    if (featuredVideo != null) {
                        item {
                            HaptiXFeaturedStimulusCard(
                                video = featuredVideo,
                                isEvaluated = featuredVideo.id in evaluatedVideoIds,
                                onClick = { onVideoSelected(featuredVideo.id) }
                            )
                        }
                    }

                    // Content Area B: Curated Reference Specimens
                    if (uiState.videos.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "CURATED REFERENCE SPECIMENS",
                                style = TechnicalMicroLabel,
                                color = colors.accentPrimary
                            )
                        }
                        items(uiState.videos, key = { it.id }) { video ->
                            HaptiXStimulusSpecimenCard(
                                video = video,
                                isSelected = video.id == uiState.selectedVideoId,
                                isEvaluated = video.id in evaluatedVideoIds,
                                onClick = { onVideoSelected(video.id) }
                            )
                        }
                    }

                    // Content Area C: Remote Study Stimulus Entry Point
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        RemoteStudyStimulusCard(
                            onLoadStimulus = { videoUrl, hapticUrl ->
                                val item = viewModel.registerRemoteStimulus(videoUrl, hapticUrl)
                                onVideoSelected(item.id)
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

/**
 * Entry point for loading remote research stimuli supplied by the research team.
 * Explicitly consumes a video URL and a corresponding Android haptic JSON URL.
 */
@Composable
fun RemoteStudyStimulusCard(
    onLoadStimulus: (videoUrl: String, hapticUrl: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HaptiXThemeTokens.colors
    var videoUrl by remember { mutableStateOf("") }
    var hapticUrl by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HaptiXShapeTokens.card)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, HaptiXShapeTokens.card)
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "REMOTE STUDY STIMULUS",
                style = TechnicalMicroLabel,
                color = colors.accentPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Evaluate a video supplied by the research team.",
                style = HaptiXBodyLarge.copy(fontSize = 13.sp, lineHeight = 18.sp),
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            HaptiXInput(
                value = videoUrl,
                onValueChange = {
                    videoUrl = it
                    if (errorMessage != null) errorMessage = null
                },
                label = "VIDEO URL",
                placeholder = "https://example.com/stimulus.mp4",
                errorMessage = errorMessage
            )

            Spacer(modifier = Modifier.height(12.dp))

            HaptiXInput(
                value = hapticUrl,
                onValueChange = { hapticUrl = it },
                label = "ANDROID HAPTIC JSON URL",
                placeholder = "https://example.com/haptics.json",
                helperText = "Direct link to research haptic dataset"
            )

            Spacer(modifier = Modifier.height(18.dp))

            HaptiXPrimaryButton(
                text = "LOAD STIMULUS →",
                onClick = {
                    val cleanVideo = videoUrl.trim()
                    if (cleanVideo.isBlank()) {
                        errorMessage = "Please enter a video URL"
                    } else if (!cleanVideo.startsWith("http://", ignoreCase = true) && !cleanVideo.startsWith("https://", ignoreCase = true)) {
                        errorMessage = "Video URL must start with http:// or https://"
                    } else {
                        errorMessage = null
                        onLoadStimulus(cleanVideo, hapticUrl.trim())
                    }
                }
            )
        }
    }
}
