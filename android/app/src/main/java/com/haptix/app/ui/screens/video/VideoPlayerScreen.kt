package com.haptix.app.ui.screens.video

import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.haptix.app.data.repository.DefaultHapticRepository
import com.haptix.app.haptics.AndroidHapticPlayer
import com.haptix.app.haptics.HapticCapabilityLevel
import com.haptix.app.haptics.HapticSynchronizer
import com.haptix.app.media.MediaResourceHelper
import com.haptix.app.ui.components.CanvasFullscreenIcon
import com.haptix.app.ui.components.CanvasPauseIcon
import com.haptix.app.ui.components.CanvasPlayIcon
import com.haptix.app.ui.components.CanvasVolumeIcon
import com.haptix.app.ui.components.HaptiXBackground
import com.haptix.app.ui.components.HapticStatusToggle
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.components.formatDuration
import com.haptix.app.ui.theme.EditorialMetadataLabel
import com.haptix.app.ui.theme.LocalSpacing
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.TechnicalValueLabel
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Screen 4: Video Stimulus Player with tactile actuation synchronization.
 * Apple-inspired media player design prioritizing visual content, clean controls,
 * native haptic actuation management, and collapsible research telemetry.
 */
@Composable
fun VideoPlayerScreen(
    videoId: String,
    viewModel: VideoPlayerViewModel = run {
        val ctx = LocalContext.current.applicationContext
        viewModel(
            key = videoId,
            factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val player = AndroidHapticPlayer(ctx)
                    val synchronizer = HapticSynchronizer(player)
                    val repo = DefaultHapticRepository(ctx)
                    @Suppress("UNCHECKED_CAST")
                    return VideoPlayerViewModel(
                        hapticRepository = repo,
                        hapticSynchronizer = synchronizer,
                        hapticEngine = player
                    ) as T
                }
            }
        )
    },
    initialHapticsEnabled: Boolean = true,
    isHapticSupported: Boolean = true,
    onHapticsToggled: (Boolean) -> Unit = {},
    onPlaybackFinished: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || uiState.isFullscreen

    // Real Media3 ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Attach ExoPlayer playback listener and ensure lifecycle-safe cleanup
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val isEnded = exoPlayer.playbackState == Player.STATE_ENDED
                viewModel.updatePlaybackState(
                    isPlaying = isPlaying,
                    isCompleted = isEnded
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val isEnded = playbackState == Player.STATE_ENDED
                viewModel.updatePlaybackState(
                    isPlaying = exoPlayer.isPlaying,
                    isCompleted = isEnded
                )
                if (playbackState == Player.STATE_READY) {
                    val realDuration = exoPlayer.duration
                    if (realDuration > 0L) {
                        viewModel.updateDuration(realDuration)
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Observe Android Lifecycle for pause/resume and position restoration
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.savePlaybackPosition(exoPlayer.currentPosition)
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    val savedPos = viewModel.getSavedPlaybackPosition()
                    if (savedPos > 0L && exoPlayer.playbackState == Player.STATE_READY) {
                        exoPlayer.seekTo(savedPos)
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Load initial metadata and video data
    LaunchedEffect(videoId) {
        viewModel.loadVideo(videoId, initialHapticsEnabled)
    }

    // Prepare media source when videoItem metadata is ready
    LaunchedEffect(uiState.videoItem) {
        uiState.videoItem?.let { video ->
            val mediaItem = MediaResourceHelper.buildMediaItem(context, video)
            if (mediaItem != null) {
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                val savedPos = viewModel.getSavedPlaybackPosition()
                if (savedPos > 0L) {
                    exoPlayer.seekTo(savedPos)
                }
            }
        }
    }

    // Millisecond-precision timeline updates while playback is active
    LaunchedEffect(uiState.isPlaying) {
        while (uiState.isPlaying) {
            val current = exoPlayer.currentPosition
            viewModel.onTimelinePositionChanged(current)
            delay(30L)
        }
    }

    // Sync volume/mute
    LaunchedEffect(uiState.isMuted) {
        exoPlayer.volume = if (uiState.isMuted) 0f else 1f
    }

    val onPlayPauseAction: () -> Unit = {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0L)
            }
            exoPlayer.play()
        }
    }

    val onSeekAction: (Long) -> Unit = { positionMs ->
        exoPlayer.seekTo(positionMs)
        viewModel.seekTo(positionMs)
    }

    val stimulusNumber = if (videoId.startsWith("video_")) {
        videoId.removePrefix("video_")
    } else {
        videoId.takeLast(2)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLandscape) {
            // Fullscreen Landscape Layout
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                VideoSurfaceArea(
                    exoPlayer = exoPlayer,
                    uiState = uiState,
                    onPlayPause = onPlayPauseAction,
                    modifier = Modifier.fillMaxSize()
                )

                // Translucent Apple-style Floating Controls at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .padding(horizontal = spacing.xl, vertical = spacing.sm)
                ) {
                    PlayerControlsBar(
                        uiState = uiState,
                        onPlayPause = onPlayPauseAction,
                        onSeek = onSeekAction,
                        onVolumeToggle = { viewModel.toggleVolume() },
                        onFullscreenToggle = { viewModel.toggleFullscreen() },
                        isLandscape = true
                    )
                }
            }
        } else {
            // Standard Portrait Layout
            HaptiXBackground(modifier = Modifier.fillMaxSize()) {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = spacing.screenHorizontal),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(spacing.md))

                        // Header: Stimulus Number and Title
                        Text(
                            text = "STIMULUS $stimulusNumber",
                            style = EditorialMetadataLabel.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = uiState.videoItem?.title ?: "Experimental Video",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(spacing.md))

                        // Dominant 20dp Rounded Video Surface Viewport hosting ExoPlayer
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(20.dp))
                        ) {
                            VideoSurfaceArea(
                                exoPlayer = exoPlayer,
                                uiState = uiState,
                                onPlayPause = onPlayPauseAction,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.md))

                        // Apple-style Playback Controls Bar
                        PlayerControlsBar(
                            uiState = uiState,
                            onPlayPause = onPlayPauseAction,
                            onSeek = onSeekAction,
                            onVolumeToggle = { viewModel.toggleVolume() },
                            onFullscreenToggle = { viewModel.toggleFullscreen() },
                            isLandscape = false
                        )

                        if (uiState.videoItem?.description?.isNotBlank() == true) {
                            Spacer(modifier = Modifier.height(spacing.sm))
                            Text(
                                text = uiState.videoItem?.description.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.sectionGap))

                        // Apple-style Native Haptic Actuation Toggle Card
                        HapticStatusToggle(
                            isHapticsEnabled = uiState.isHapticsEnabled,
                            isHapticSupported = isHapticSupported,
                            capabilityLevel = uiState.capabilityLevel,
                            isPlaying = uiState.isPlaying && uiState.currentEventFrequencyHz != null,
                            onToggleChanged = { enabled ->
                                viewModel.setHapticsEnabled(enabled)
                                onHapticsToggled(enabled)
                            }
                        )

                        Spacer(modifier = Modifier.height(spacing.sm))

                        // Subtle Research Telemetry Toggle (Dev/Researcher only)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = if (uiState.isDebugMode) "Hide Research Telemetry" else "Research Telemetry",
                                style = EditorialMetadataLabel.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .clickable { viewModel.toggleDebugMode() }
                                    .padding(vertical = spacing.xs)
                            )
                        }

                        if (uiState.isDebugMode) {
                            Spacer(modifier = Modifier.height(spacing.xs))
                            ResearchDebugHudCard(uiState = uiState)
                        }

                        Spacer(modifier = Modifier.height(spacing.sectionGap))
                    }

                    // Bottom Progression Button
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.xl)
                    ) {
                        HaptiXPrimaryButton(
                            text = "PROCEED TO FEEDBACK  →",
                            onClick = { onPlaybackFinished(videoId) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Visual canvas area for video stimulus presentation hosting Media3 [PlayerView].
 */
@Composable
private fun VideoSurfaceArea(
    exoPlayer: ExoPlayer,
    uiState: VideoPlayerUiState,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Real Media3 PlayerView hosting the video surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Center Tap to Play/Pause Floating Affordance (shown when paused or completed)
        if (!uiState.isPlaying) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(onClick = onPlayPause)
                    .semantics {
                        this.contentDescription = if (uiState.isPlaying) "Pause video" else "Play video"
                    }
            ) {
                CanvasPlayIcon(color = Color.White, size = 24.dp)
            }
        }
    }
}

/**
 * Apple-style clean playback controls bar with scrubber, time labels, and circular actions.
 */
@Composable
private fun PlayerControlsBar(
    uiState: VideoPlayerUiState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolumeToggle: () -> Unit,
    onFullscreenToggle: () -> Unit,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val contentColor = if (isLandscape) Color.White else MaterialTheme.colorScheme.onBackground

    Column(modifier = modifier.fillMaxWidth()) {
        // Scrubber Slider
        val maxDuration = uiState.durationMs.coerceAtLeast(1L).toFloat()
        val currentPosition = uiState.currentPositionMs.toFloat().coerceIn(0f, maxDuration)

        Slider(
            value = currentPosition,
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..maxDuration,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = if (isLandscape) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    this.contentDescription = "Playback seek slider, ${formatDuration(uiState.currentPositionMs)} of ${formatDuration(uiState.durationMs)}"
                }
        )

        // Time and Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Play/Pause button + Current / Total Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(44.dp)
                ) {
                    if (uiState.isPlaying) {
                        CanvasPauseIcon(color = contentColor, size = 20.dp)
                    } else {
                        CanvasPlayIcon(color = contentColor, size = 20.dp)
                    }
                }

                Spacer(modifier = Modifier.width(spacing.xs))

                Text(
                    text = "${formatDuration(uiState.currentPositionMs)} / ${formatDuration(uiState.durationMs)}",
                    style = EditorialMetadataLabel.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isLandscape) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right: Volume and Fullscreen buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onVolumeToggle,
                    modifier = Modifier.size(44.dp)
                ) {
                    CanvasVolumeIcon(color = contentColor, isMuted = uiState.isMuted, size = 20.dp)
                }

                IconButton(
                    onClick = onFullscreenToggle,
                    modifier = Modifier.size(44.dp)
                ) {
                    CanvasFullscreenIcon(color = contentColor, isFullscreen = uiState.isFullscreen, size = 20.dp)
                }
            }
        }
    }
}

/**
 * Compact Research Debug HUD displaying real-time synchronization and frequency telemetry.
 * Subtle, non-intrusive container reserved for scientific calibration.
 */
@Composable
private fun ResearchDebugHudCard(
    uiState: VideoPlayerUiState,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val posMs = uiState.currentPositionMs
    val totalSec = posMs / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    val millis = posMs % 1000
    val formattedPosition = String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)

    val freqText = uiState.currentEventFrequencyHz?.let { String.format(Locale.US, "%.1f Hz", it) } ?: "---"
    val ampText = uiState.currentEventAmplitude?.let { String.format(Locale.US, "%.2f", it) } ?: "---"
    val durText = uiState.currentEventDurationMs?.let { "${it} ms" } ?: "---"
    val eventLabel = uiState.currentEventDescription ?: (if (uiState.currentEventFrequencyHz != null) "Active Stimulus" else "Idle / Between Events")

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESEARCH TELEMETRY",
                    style = EditorialMetadataLabel.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.0.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "DEBUG MODE",
                    style = EditorialMetadataLabel.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            DebugTelemetryRow(label = "Video Position", value = formattedPosition)
            DebugTelemetryRow(label = "Haptic Event", value = eventLabel)
            DebugTelemetryRow(label = "Frequency", value = freqText)
            DebugTelemetryRow(label = "Amplitude", value = ampText)
            DebugTelemetryRow(label = "Duration", value = durText)
            DebugTelemetryRow(
                label = "Capability",
                value = uiState.capabilityLevel.name,
                valueColor = when (uiState.capabilityLevel) {
                    HapticCapabilityLevel.SUPPORTED -> Color(0xFF30D158)
                    HapticCapabilityLevel.LIMITED -> Color(0xFFFFD60A)
                    HapticCapabilityLevel.UNAVAILABLE -> Color(0xFFFF453A)
                }
            )
        }
    }
}

@Composable
private fun DebugTelemetryRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = EditorialMetadataLabel.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = valueColor
        )
    }
}
