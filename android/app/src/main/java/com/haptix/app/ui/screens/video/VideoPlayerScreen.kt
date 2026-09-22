package com.haptix.app.ui.screens.video

import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.haptix.app.media.MediaResourceHelper
import com.haptix.app.ui.components.CanvasFullscreenIcon
import com.haptix.app.ui.components.CanvasPauseIcon
import com.haptix.app.ui.components.CanvasPlayIcon
import com.haptix.app.ui.components.CanvasVolumeIcon
import com.haptix.app.ui.components.CyberBackground
import com.haptix.app.ui.components.HapticStatusToggle
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.components.formatDuration
import com.haptix.app.ui.theme.CyberCyan
import com.haptix.app.ui.theme.CyberGreen
import com.haptix.app.ui.theme.LocalSpacing
import com.haptix.app.ui.theme.Slate950
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.TechnicalValueLabel
import kotlinx.coroutines.delay

import androidx.compose.foundation.clickable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.haptix.app.data.repository.DefaultHapticRepository
import com.haptix.app.haptics.AndroidHapticPlayer
import com.haptix.app.haptics.HapticCapabilityLevel
import com.haptix.app.haptics.HapticSynchronizer
import com.haptix.app.ui.theme.CyberAmber
import com.haptix.app.ui.theme.CyberTextMuted

/**
 * Screen 4: Video Stimulus Player with tactile actuation synchronization.
 * Uses AndroidX Media3 ExoPlayer for real video playback, connected to custom streaming-grade
 * floating controls and millisecond-accurate haptic synchronization.
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
                    isLandscape = true,
                    modifier = Modifier.fillMaxSize()
                )

                // Floating Streaming Controls at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.9f)
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
            // Standard Portrait Layout with Cyber Background
            CyberBackground(modifier = Modifier.fillMaxSize()) {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.lg)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(spacing.md))

                        // Editorial Step Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "03 // STIMULUS PLAYBACK",
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
                                    text = "03 / 04",
                                    style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(spacing.xs))

                        Text(
                            text = "Tactile Presentation",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Text(
                            text = "Observe the audio-visual presentation and evaluate tactile actuation.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(spacing.md))

                        // Dominant 22dp Rounded Video Surface Viewport hosting ExoPlayer
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            color = Slate950,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(22.dp))
                        ) {
                            VideoSurfaceArea(
                                exoPlayer = exoPlayer,
                                uiState = uiState,
                                onPlayPause = onPlayPauseAction,
                                isLandscape = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Floating Streaming Controls Bar
                        Spacer(modifier = Modifier.height(spacing.xs))

                        PlayerControlsBar(
                            uiState = uiState,
                            onPlayPause = onPlayPauseAction,
                            onSeek = onSeekAction,
                            onVolumeToggle = { viewModel.toggleVolume() },
                            onFullscreenToggle = { viewModel.toggleFullscreen() },
                            isLandscape = false
                        )

                        Spacer(modifier = Modifier.height(spacing.md))

                        // Stimulus Metadata Card (20dp rounded)
                        uiState.videoItem?.let { video ->
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
                                            text = "[ STIMULUS METADATA // ${video.id.uppercase()} ]",
                                            style = TechnicalMicroLabel,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = formatDuration(video.durationMs),
                                            style = TechnicalValueLabel,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(spacing.xs))

                                    Text(
                                        text = video.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (video.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(spacing.xxs))
                                        Text(
                                            text = video.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(spacing.md))

                        // Persistent Haptic Actuation Toggle Card with Pulse Line
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

                        Spacer(modifier = Modifier.height(spacing.xs))

                        // Subtle Research Telemetry Toggle (Dev/Researcher only)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = if (uiState.isDebugMode) "[-] HIDE RESEARCH TELEMETRY" else "[+] RESEARCH DEBUG MODE",
                                style = TechnicalMicroLabel.copy(fontSize = 9.sp),
                                color = CyberTextMuted,
                                modifier = Modifier
                                    .clickable { viewModel.toggleDebugMode() }
                                    .padding(vertical = spacing.xxs)
                            )
                        }

                        if (uiState.isDebugMode) {
                            Spacer(modifier = Modifier.height(spacing.xs))
                            ResearchDebugHudCard(uiState = uiState)
                        }

                        Spacer(modifier = Modifier.height(spacing.lg))
                    }

                    // Completion Progression Action
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = spacing.xl)
                    ) {
                        HaptiXPrimaryButton(
                            text = if (uiState.isCompleted) "PROCEED TO FEEDBACK  →" else "EVALUATE STIMULUS (FEEDBACK)  →",
                            onClick = { onPlaybackFinished(videoId) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Visual canvas area for video stimulus presentation hosting Media3 [PlayerView] with HUD overlay.
 */
@Composable
private fun VideoSurfaceArea(
    exoPlayer: ExoPlayer,
    uiState: VideoPlayerUiState,
    onPlayPause: () -> Unit,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val stimulusTag = if (uiState.videoItem?.id?.startsWith("video_") == true) {
        "STIMULUS " + uiState.videoItem.id.removePrefix("video_")
    } else {
        "STIMULUS // ${uiState.videoItem?.id?.uppercase() ?: "01"}"
    }

    val syncStatusText = when {
        uiState.isPlaying -> "PLAYING"
        uiState.isCompleted -> "COMPLETED"
        else -> "PAUSED"
    }
    val syncStatusColor = when {
        uiState.isPlaying -> CyberGreen
        uiState.isCompleted -> CyberCyan
        else -> Color.White.copy(alpha = 0.5f)
    }

    Box(
        modifier = modifier.background(Slate950),
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

        // Floating Top HUD Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = stimulusTag,
                    style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                    color = CyberCyan
                )
                Text(
                    text = (uiState.videoItem?.title ?: "Experimental Stimulus").uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
            }

            // Sync Status Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(syncStatusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SYNC: $syncStatusText",
                    style = TechnicalMicroLabel.copy(fontSize = 9.sp),
                    color = syncStatusColor
                )
            }
        }

        // Center Tap to Play/Pause Floating Affordance (shown when paused or completed)
        if (!uiState.isPlaying) {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .semantics {
                        this.contentDescription = if (uiState.isPlaying) "Pause video" else "Play video"
                    }
            ) {
                CanvasPlayIcon(color = Color.White, size = 28.dp)
            }
        }
    }
}

/**
 * Reusable streaming-grade playback controls bar.
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
    val contentColor = if (isLandscape) Color.White else MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier.fillMaxWidth()) {
        // Sleek Scrubber Slider
        val maxDuration = uiState.durationMs.coerceAtLeast(1L).toFloat()
        val currentPosition = uiState.currentPositionMs.toFloat().coerceIn(0f, maxDuration)

        Slider(
            value = currentPosition,
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..maxDuration,
            colors = SliderDefaults.colors(
                thumbColor = CyberCyan,
                activeTrackColor = CyberCyan,
                inactiveTrackColor = if (isLandscape) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    this.contentDescription = "Playback seek slider, ${formatDuration(uiState.currentPositionMs)} of ${formatDuration(uiState.durationMs)}"
                }
        )

        // Time and Floating Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Play / Pause Floating Button
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

                // Time indicators in monospace technical style
                Text(
                    text = "${formatDuration(uiState.currentPositionMs)} / ${formatDuration(uiState.durationMs)}",
                    style = TechnicalValueLabel.copy(fontSize = 12.sp),
                    color = contentColor
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Volume Mute / Unmute Floating Button
                IconButton(
                    onClick = onVolumeToggle,
                    modifier = Modifier.size(44.dp)
                ) {
                    CanvasVolumeIcon(color = contentColor, isMuted = uiState.isMuted, size = 20.dp)
                }

                // Fullscreen Floating Button
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
 * Technical Research Debug HUD displaying real-time synchronization and frequency parameters.
 * Accessible to researchers via the technical toggle; hidden from normal study participants.
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
    val formattedPosition = String.format("%02d:%02d.%03d", minutes, seconds, millis)

    val freqText = uiState.currentEventFrequencyHz?.let { String.format("%.1f Hz", it) } ?: "---"
    val ampText = uiState.currentEventAmplitude?.let { String.format("%.2f", it) } ?: "---"
    val durText = uiState.currentEventDurationMs?.let { "${it} ms" } ?: "---"
    val eventLabel = uiState.currentEventDescription ?: (if (uiState.currentEventFrequencyHz != null) "ACTIVE STIMULUS" else "IDLE / BETWEEN EVENTS")

    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESEARCH DEBUG // REAL-TIME TELEMETRY",
                    style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                    color = CyberCyan
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "DEV ONLY",
                        style = TechnicalMicroLabel.copy(fontSize = 8.sp),
                        color = CyberCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.xs))

            DebugTelemetryRow(label = "Current video position", value = formattedPosition)
            DebugTelemetryRow(label = "Current haptic event", value = eventLabel)
            DebugTelemetryRow(label = "Frequency", value = freqText)
            DebugTelemetryRow(label = "Amplitude", value = ampText)
            DebugTelemetryRow(label = "Event duration", value = durText)
            DebugTelemetryRow(
                label = "Haptic capability",
                value = uiState.capabilityLevel.name,
                valueColor = when (uiState.capabilityLevel) {
                    HapticCapabilityLevel.SUPPORTED -> CyberGreen
                    HapticCapabilityLevel.LIMITED -> CyberAmber
                    HapticCapabilityLevel.UNAVAILABLE -> Color(0xFFEF4444)
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
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = TechnicalMicroLabel.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = TechnicalValueLabel.copy(fontSize = 11.sp),
            color = valueColor
        )
    }
}

