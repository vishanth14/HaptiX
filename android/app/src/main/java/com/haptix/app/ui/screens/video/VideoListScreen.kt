package com.haptix.app.ui.screens.video

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.haptix.app.ui.components.CyberBackground
import com.haptix.app.ui.components.HaptiXEmptyState
import com.haptix.app.ui.components.HaptiXErrorMessage
import com.haptix.app.ui.components.HaptiXLoadingIndicator
import com.haptix.app.ui.components.VideoCard
import com.haptix.app.ui.theme.LocalSpacing
import com.haptix.app.ui.theme.TechnicalMicroLabel

/**
 * Screen 3: Video Stimuli Selection.
 * Presents research participants with large premium stimulus cards featuring abstract waveform banners.
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
        CyberBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.lg)
            ) {
                Spacer(modifier = Modifier.height(spacing.md))

                // Editorial Header with Step Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "02 // STIMULUS LIBRARY",
                        style = TechnicalMicroLabel.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "02 / 04",
                            style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.xs))

                Text(
                    text = "Experimental Stimuli",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Select a clip to initialize synchronized audio-visual and tactile playback.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(spacing.lg))

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
                            modifier = Modifier.fillMaxSize(),
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
                            modifier = Modifier.fillMaxSize(),
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
                            verticalArrangement = Arrangement.spacedBy(spacing.lg),
                            contentPadding = PaddingValues(bottom = spacing.xl)
                        ) {
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
}
