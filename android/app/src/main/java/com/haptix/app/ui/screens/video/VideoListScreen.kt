package com.haptix.app.ui.screens.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haptix.app.ui.components.FeaturedStimulusCard
import com.haptix.app.ui.components.HaptiXBackground
import com.haptix.app.ui.components.HaptiXEmptyState
import com.haptix.app.ui.components.HaptiXErrorMessage
import com.haptix.app.ui.components.HaptiXLoadingIndicator
import com.haptix.app.ui.components.VideoCard
import com.haptix.app.ui.theme.EditorialMetadataLabel
import com.haptix.app.ui.theme.LocalSpacing

/**
 * Screen 3: Video Stimuli Selection.
 * Premium Apple Music-inspired media browsing library for multimodal research.
 * Features a prominent editorial Featured Stimulus hero and a spacious list of All Stimuli.
 */
@Composable
fun VideoListScreen(
    viewModel: VideoListViewModel = viewModel(),
    onVideoSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        HaptiXBackground {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        HaptiXLoadingIndicator(message = "Calibrating experimental stimuli...")
                    }
                }

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = spacing.screenHorizontal),
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
                            .padding(horizontal = spacing.screenHorizontal),
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
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = spacing.screenHorizontal,
                            end = spacing.screenHorizontal,
                            top = spacing.md,
                            bottom = spacing.largeSectionGap
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.cardGap)
                    ) {
                        // Section 1: Editorial Header
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "02",
                                    style = EditorialMetadataLabel.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Stimulus Library",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Select a video stimulus.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(spacing.sectionGap))
                            }
                        }

                        // Section 2: Featured Stimulus
                        if (uiState.videos.isNotEmpty()) {
                            val featuredVideo = uiState.videos.first()
                            item {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Featured Stimulus",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    FeaturedStimulusCard(
                                        video = featuredVideo,
                                        onClick = {
                                            viewModel.selectVideo(featuredVideo.id)
                                            onVideoSelected(featuredVideo.id)
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(spacing.sectionGap))
                                }
                            }
                        }

                        // Section 3: All Stimuli Section Heading
                        item {
                            Text(
                                text = "All Stimuli",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }

                        // Section 4: All Stimuli Items
                        items(
                            items = uiState.videos,
                            key = { it.id }
                        ) { video ->
                            VideoCard(
                                video = video,
                                isSelected = video.id == uiState.selectedVideoId,
                                onClick = {
                                    viewModel.selectVideo(video.id)
                                    onVideoSelected(video.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
