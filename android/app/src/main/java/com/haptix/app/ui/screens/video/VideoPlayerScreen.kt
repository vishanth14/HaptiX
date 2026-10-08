package com.haptix.app.ui.screens.video

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticEventType
import com.haptix.app.data.model.SemanticHapticType
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
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
import com.haptix.app.ui.components.GlassControl
import com.haptix.app.ui.components.GlassSurface
import com.haptix.app.ui.components.HaptiXBackground
import com.haptix.app.ui.components.HapticStatusToggle
import com.haptix.app.ui.components.HaptiXPrimaryButton
import com.haptix.app.ui.components.HaptiXProtocolEyebrow
import com.haptix.app.ui.components.formatDuration
import com.haptix.app.ui.components.motion.MagicIcon
import com.haptix.app.ui.components.motion.bounceClick
import com.haptix.app.ui.components.motion.elasticOverscroll
import com.haptix.app.ui.theme.EditorialMetadataLabel
import com.haptix.app.ui.theme.HaptiXShapeTokens
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.LocalSpacing
import com.haptix.app.ui.theme.MaterialLevel
import com.haptix.app.ui.theme.MaterialTokens
import com.haptix.app.ui.theme.ObsidianBalthazarFontFamily
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.rememberUiHaptics
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Screen 4: Video Stimulus Player with tactile actuation synchronization.
 * Apple-inspired media player design with MagicIcon glyph morphing, spring physics controls,
 * translucent material surfaces, native UI tactile feedback, and collapsible research telemetry.
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
                    val monitor = com.haptix.app.monitoring.PerformanceMonitor(context = ctx)
                    @Suppress("UNCHECKED_CAST")
                    return VideoPlayerViewModel(
                        hapticRepository = repo,
                        hapticSynchronizer = synchronizer,
                        performanceMonitor = monitor,
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
    onPlaybackFinishedWithMetrics: (String, List<com.haptix.app.data.model.PerformanceMetric>) -> Unit = { id, _ -> onPlaybackFinished(id) },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val spacing = LocalSpacing.current
    val uiHaptics = rememberUiHaptics()
    val uiState by viewModel.uiState.collectAsState()
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || uiState.isFullscreen

    // Track user interaction timestamp for auto-hiding controls while playing
    var lastInteractionTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    val onResetInactivityTimer = {
        lastInteractionTimestamp = System.currentTimeMillis()
    }

    val onToggleControls = {
        val nextVisible = !uiState.controlsVisible
        viewModel.setControlsVisibility(nextVisible)
        if (nextVisible) {
            lastInteractionTimestamp = System.currentTimeMillis()
        }
    }

    // Auto-hide controller after 3000ms of inactivity while actively playing
    LaunchedEffect(uiState.isPlaying, uiState.controlsVisible, lastInteractionTimestamp) {
        if (uiState.isPlaying && uiState.controlsVisible) {
            delay(3000L)
            viewModel.setControlsVisibility(false)
        }
    }

    // When playback transitions to paused or completed, ensure controls are visible
    LaunchedEffect(uiState.isPlaying, uiState.isCompleted) {
        if (!uiState.isPlaying || uiState.isCompleted) {
            viewModel.setControlsVisibility(true)
        }
    }

    // Manage immersive sticky fullscreen (hide status and navigation bars in landscape)
    DisposableEffect(isLandscape, activity) {
        PlayerSystemUiHelper.setFullscreen(activity, isLandscape)
        onDispose {
            PlayerSystemUiHelper.setFullscreen(activity, false)
        }
    }

    // Ensure orientation and system UI are cleanly restored upon leaving player screen
    DisposableEffect(activity) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            PlayerSystemUiHelper.setFullscreen(activity, false)
        }
    }

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
                val isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING
                viewModel.updatePlaybackState(
                    isPlaying = isPlaying,
                    isCompleted = isEnded,
                    isBuffering = isBuffering,
                    currentPositionMs = exoPlayer.currentPosition
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val isEnded = playbackState == Player.STATE_ENDED
                val isBuffering = playbackState == Player.STATE_BUFFERING
                viewModel.updatePlaybackState(
                    isPlaying = exoPlayer.isPlaying,
                    isCompleted = isEnded,
                    isBuffering = isBuffering,
                    currentPositionMs = exoPlayer.currentPosition
                )
                if (playbackState == Player.STATE_READY) {
                    val realDuration = exoPlayer.duration
                    if (realDuration > 0L) {
                        viewModel.updateDuration(realDuration)
                    }
                }
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                val realDuration = exoPlayer.duration
                if (realDuration > 0L) {
                    viewModel.updateDuration(realDuration)
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                val targetMs = newPosition.positionMs
                viewModel.onPlayerPositionDiscontinuity(oldPosition.positionMs, targetMs, reason)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val errorMsg = error.localizedMessage ?: "Video stream error (${error.errorCode})"
                viewModel.onPlaybackError(errorMsg)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Observe Android Lifecycle for pause/resume and position restoration
    DisposableEffect(lifecycleOwner, exoPlayer, isLandscape, activity) {
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
                    PlayerSystemUiHelper.reapplyIfFullscreen(activity, isLandscape)
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
            } else {
                viewModel.onPlaybackError("Unable to resolve media source: ${video.resolvedVideoUri}")
            }
        }
    }

    // Millisecond-precision timeline updates while playback is active (~40Hz polling)
    LaunchedEffect(uiState.isPlaying) {
        while (uiState.isPlaying) {
            if (exoPlayer.isPlaying && exoPlayer.playbackState == Player.STATE_READY) {
                val current = exoPlayer.currentPosition
                viewModel.onTimelinePositionChanged(current, isFromExternalPlayer = true)
            }
            delay(25L)
        }
    }

    // Sync volume/mute
    LaunchedEffect(uiState.isMuted) {
        exoPlayer.volume = if (uiState.isMuted) 0f else 1f
    }

    val onPlayPauseAction: () -> Unit = {
        uiHaptics.tap()
        onResetInactivityTimer()
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
        onResetInactivityTimer()
        exoPlayer.seekTo(positionMs)
        viewModel.seekTo(positionMs)
    }

    val onFullscreenToggleAction: () -> Unit = {
        uiHaptics.tap()
        onResetInactivityTimer()
        val nextFullscreen = !uiState.isFullscreen
        viewModel.toggleFullscreen()
        if (nextFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
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
                    onVideoAreaTapped = onToggleControls,
                    onPlayPause = onPlayPauseAction,
                    modifier = Modifier.fillMaxSize()
                )

                // Translucent Apple-style Floating Controls at bottom with auto-hide animation
                AnimatedVisibility(
                    visible = uiState.controlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
                            onVolumeToggle = {
                                uiHaptics.tap()
                                onResetInactivityTimer()
                                viewModel.toggleVolume()
                            },
                            onFullscreenToggle = onFullscreenToggleAction,
                            isLandscape = true
                        )
                    }
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
                        .elasticOverscroll()
                        .padding(horizontal = spacing.screenHorizontal),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(18.dp))

                        // Header: Stimulus Specimen Code and Title
                        HaptiXProtocolEyebrow(text = "SPECIMEN // STIMULUS $stimulusNumber")

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = uiState.videoItem?.title ?: "Experimental Video",
                            fontFamily = ObsidianBalthazarFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = HaptiXThemeTokens.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dominant 16dp Rounded Video Surface Viewport hosting ExoPlayer
                        Surface(
                            shape = HaptiXShapeTokens.media,
                            color = Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(HaptiXShapeTokens.media)
                        ) {
                            VideoSurfaceArea(
                                exoPlayer = exoPlayer,
                                uiState = uiState,
                                onVideoAreaTapped = onToggleControls,
                                onPlayPause = onPlayPauseAction,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Translucent Playback Controls Bar with MagicIcon
                        AnimatedVisibility(
                            visible = uiState.controlsVisible,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            PlayerControlsBar(
                                uiState = uiState,
                                onPlayPause = onPlayPauseAction,
                                onSeek = onSeekAction,
                                onVolumeToggle = {
                                    uiHaptics.tap()
                                    onResetInactivityTimer()
                                    viewModel.toggleVolume()
                                },
                                onFullscreenToggle = onFullscreenToggleAction,
                                isLandscape = false
                            )
                        }

                        if (uiState.videoItem?.description?.isNotBlank() == true) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = uiState.videoItem?.description.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = HaptiXThemeTokens.colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Tactile Actuation Output Instrument Card
                        HapticStatusToggle(
                            isHapticsEnabled = uiState.isHapticsEnabled,
                            isHapticSupported = isHapticSupported,
                            capabilityLevel = uiState.capabilityLevel,
                            isPlaying = uiState.isPlaying && uiState.currentEventFrequencyHz != null,
                            currentFrequencyHz = uiState.currentEventFrequencyHz,
                            currentAmplitude = uiState.currentEventAmplitude,
                            onToggleChanged = { enabled ->
                                viewModel.setHapticsEnabled(enabled)
                                onHapticsToggled(enabled)
                            }
                        )

                        if (uiState.errorMessage != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "STREAM ALERT: ${uiState.errorMessage}",
                                color = Color(0xFFFF5555),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (uiState.hapticErrorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "HAPTIC STREAM ALERT: ${uiState.hapticErrorMessage}",
                                color = Color(0xFFFFB86C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Research Diagnostics & Developer Telemetry Controls
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(HaptiXShapeTokens.card)
                                .background(HaptiXThemeTokens.colors.surface)
                                .border(1.dp, HaptiXThemeTokens.colors.borderSubtle, HaptiXShapeTokens.card)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RESEARCH DIAGNOSTICS",
                                    style = TechnicalMicroLabel,
                                    color = HaptiXThemeTokens.colors.textSecondary
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Haptic Debug Toggle Chip
                                    val isHapticHudOn = uiState.isHapticDebugOverlayVisible
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(HaptiXShapeTokens.micro)
                                            .background(if (isHapticHudOn) HaptiXThemeTokens.colors.focusSurface else HaptiXThemeTokens.colors.controlSurface)
                                            .border(
                                                1.dp,
                                                if (isHapticHudOn) HaptiXThemeTokens.colors.accentPrimary else HaptiXThemeTokens.colors.borderSubtle,
                                                HaptiXShapeTokens.micro
                                            )
                                            .clickable { viewModel.toggleHapticDebugOverlay() }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isHapticHudOn) "HAPTIC HUD [ON]" else "HAPTIC HUD",
                                            style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                                            color = if (isHapticHudOn) HaptiXThemeTokens.colors.accentPrimary else HaptiXThemeTokens.colors.textSecondary
                                        )
                                    }

                                    // Hardware Telemetry Toggle Chip
                                    val isTelemetryOn = uiState.isDebugMode
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .clip(HaptiXShapeTokens.micro)
                                            .background(if (isTelemetryOn) HaptiXThemeTokens.colors.focusSurface else HaptiXThemeTokens.colors.controlSurface)
                                            .border(
                                                1.dp,
                                                if (isTelemetryOn) HaptiXThemeTokens.colors.accentPrimary else HaptiXThemeTokens.colors.borderSubtle,
                                                HaptiXShapeTokens.micro
                                            )
                                            .clickable { viewModel.toggleDebugMode() }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isTelemetryOn) "TELEMETRY [ON]" else "TELEMETRY",
                                            style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                                            color = if (isTelemetryOn) HaptiXThemeTokens.colors.accentPrimary else HaptiXThemeTokens.colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.isHapticDebugOverlayVisible) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HapticDebugOverlayCard(
                                uiState = uiState,
                                onTestTimestamp = { ts -> viewModel.testEventAtTimestamp(ts) }
                            )
                        }

                        if (uiState.isDebugMode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            ResearchDebugHudCard(
                                uiState = uiState,
                                onTestHaptic = { viewModel.testHapticActuation() }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Bottom Progression Button
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        HaptiXPrimaryButton(
                            text = "PROCEED TO FEEDBACK  →",
                            onClick = {
                                uiHaptics.confirm()
                                val metrics = viewModel.getRecordedPerformanceMetrics()
                                onPlaybackFinishedWithMetrics(videoId, metrics)
                            }
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
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun VideoSurfaceArea(
    exoPlayer: ExoPlayer,
    uiState: VideoPlayerUiState,
    onVideoAreaTapped: () -> Unit,
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
                    isClickable = false
                    isFocusable = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        // Persistent full-surface tap layer for reliable control toggling.
        // Positioned above AndroidView in Compose Z-order to ensure taps are never lost
        // whether controls are visible or hidden.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onVideoAreaTapped
                )
                .semantics {
                    this.contentDescription = if (uiState.controlsVisible) {
                        "Hide player controls"
                    } else {
                        "Show player controls"
                    }
                }
        )

        // Center Tap to Play/Pause Floating Affordance: only shown when PAUSED AND controls are visible!
        // Never shown while playing!
        if (!uiState.isPlaying && uiState.controlsVisible) {
            GlassControl(
                shape = CircleShape,
                level = MaterialLevel.Regular,
                onClick = onPlayPause,
                modifier = Modifier
                    .size(56.dp)
                    .semantics {
                        this.contentDescription = "Play video"
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp)
                ) {
                    CanvasPlayIcon(color = Color.White, size = 24.dp)
                }
            }
        }
    }
}

/**
 * Apple-style clean playback controls bar with scrubber, haptic timeline density track,
 * time labels, forward/backward seek controls, and MagicIcon glyph transitions.
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
    val colors = HaptiXThemeTokens.colors

    Column(modifier = modifier.fillMaxWidth()) {
        // Scrubber Slider
        val maxDuration = uiState.durationMs.coerceAtLeast(1L).toFloat()
        val currentPosition = uiState.currentPositionMs.toFloat().coerceIn(0f, maxDuration)

        Slider(
            value = currentPosition,
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..maxDuration,
            colors = SliderDefaults.colors(
                thumbColor = colors.accentPrimary,
                activeTrackColor = colors.accentPrimary,
                inactiveTrackColor = if (isLandscape) Color.White.copy(alpha = 0.25f) else colors.borderSubtle
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    this.contentDescription = "Playback seek slider, ${formatDuration(uiState.currentPositionMs)} of ${formatDuration(uiState.durationMs)}"
                }
        )

        // Synchronized Haptic Event Density Playline Track (Exact Timestamps from Pattern Events)
        HapticPlaylineMarkersTrack(
            events = uiState.hapticEvents,
            durationMs = uiState.durationMs,
            currentPositionMs = uiState.currentPositionMs,
            onSeek = onSeek,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Time and Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Play/Pause button + Current / Total Time + Quick Seek Steps
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GlassControl(
                    onClick = onPlayPause,
                    shape = CircleShape,
                    level = MaterialLevel.Thin,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics {
                            this.contentDescription = if (uiState.isPlaying) "Pause" else "Play"
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(40.dp)
                    ) {
                        MagicIcon(targetState = uiState.isPlaying) { isPlaying ->
                            if (isPlaying) {
                                CanvasPauseIcon(color = contentColor, size = 18.dp)
                            } else {
                                CanvasPlayIcon(color = contentColor, size = 18.dp)
                            }
                        }
                    }
                }

                // -10s Seek Button
                GlassControl(
                    onClick = {
                        val newPos = (uiState.currentPositionMs - 10000L).coerceAtLeast(0L)
                        onSeek(newPos)
                    },
                    shape = CircleShape,
                    level = MaterialLevel.Thin,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(34.dp)) {
                        Text(
                            text = "-10",
                            style = TechnicalMicroLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = contentColor
                        )
                    }
                }

                // +10s Seek Button
                GlassControl(
                    onClick = {
                        val newPos = (uiState.currentPositionMs + 10000L).coerceAtMost(uiState.durationMs)
                        onSeek(newPos)
                    },
                    shape = CircleShape,
                    level = MaterialLevel.Thin,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(34.dp)) {
                        Text(
                            text = "+10",
                            style = TechnicalMicroLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = contentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "${formatDuration(uiState.currentPositionMs)} / ${formatDuration(uiState.durationMs)}",
                    style = EditorialMetadataLabel.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isLandscape) Color.White.copy(alpha = 0.8f) else colors.textSecondary
                )
            }

            // Right: Volume and Fullscreen buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassControl(
                    onClick = onVolumeToggle,
                    shape = CircleShape,
                    level = MaterialLevel.Thin,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics {
                            this.contentDescription = if (uiState.isMuted) "Unmute" else "Mute"
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(40.dp)
                    ) {
                        MagicIcon(targetState = uiState.isMuted) { isMuted ->
                            CanvasVolumeIcon(color = contentColor, isMuted = isMuted, size = 18.dp)
                        }
                    }
                }

                GlassControl(
                    onClick = onFullscreenToggle,
                    shape = CircleShape,
                    level = MaterialLevel.Thin,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics {
                            this.contentDescription = if (uiState.isFullscreen) "Exit fullscreen" else "Enter fullscreen"
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(40.dp)
                    ) {
                        MagicIcon(targetState = uiState.isFullscreen) { isFullscreen ->
                            CanvasFullscreenIcon(color = contentColor, isFullscreen = isFullscreen, size = 18.dp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Synchronized Haptic Playline Density Track representing exact tactile event impulses
 * parsed from the authoritative timeline (e.g. f1_haptic_timeline.json).
 *
 * Renders each event in [events] at its exact timestamp ([HapticEvent.peakTimeMs] / [HapticEvent.startTimeMs]),
 * scaling amplitude, duration, and semantic characteristics with active playhead highlighting.
 */
@Composable
fun HapticPlaylineMarkersTrack(
    events: List<HapticEvent>,
    durationMs: Long,
    currentPositionMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HaptiXThemeTokens.colors
    val totalDurationMs = durationMs.coerceAtLeast(1L).toFloat()

    Box(
        modifier = modifier
            .pointerInput(totalDurationMs) {
                detectTapGestures { offset ->
                    if (size.width > 0) {
                        val seekTargetMs = ((offset.x / size.width) * totalDurationMs)
                            .toLong()
                            .coerceIn(0L, durationMs)
                        onSeek(seekTargetMs)
                    }
                }
            }
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            // 1. Subtle reference timeline base line
            drawLine(
                color = colors.borderSubtle.copy(alpha = 0.35f),
                start = Offset(0f, h / 2f),
                end = Offset(w, h / 2f),
                strokeWidth = 1.dp.toPx()
            )

            if (events.isEmpty()) return@Canvas

            // 2. Continuous / Sustained Rumble Spans
            for (event in events) {
                val dur = event.durationMs
                if (dur > 60L || event.type == HapticEventType.CONTINUOUS) {
                    val startX = ((event.startTimeMs.toFloat() / totalDurationMs) * w).coerceIn(0f, w)
                    val endX = (((event.startTimeMs + dur).toFloat() / totalDurationMs) * w).coerceIn(0f, w)
                    val spanWidth = (endX - startX).coerceAtLeast(2.dp.toPx())

                    val isActive = currentPositionMs in event.startTimeMs..(event.startTimeMs + dur)
                    val isPast = currentPositionMs > (event.startTimeMs + dur)

                    val spanAlpha = when {
                        isActive -> 0.80f
                        isPast -> 0.40f
                        else -> 0.22f
                    }
                    val spanHeight = h * 0.45f
                    drawRoundRect(
                        color = colors.accentPrimary.copy(alpha = spanAlpha),
                        topLeft = Offset(startX, (h - spanHeight) / 2f),
                        size = Size(spanWidth.coerceAtMost(w - startX), spanHeight),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                }
            }

            // 3. Exact Tactile Impulse Markers (Authoritative timestamp = event.startTimeMs)
            for (event in events) {
                val markerTimestampMs = event.startTimeMs
                val markerX = ((markerTimestampMs.toFloat() / totalDurationMs) * w).coerceIn(0f, w)

                val isActive = currentPositionMs in event.startTimeMs..(event.startTimeMs + event.durationMs.coerceAtLeast(100L))
                val isPast = currentPositionMs > (event.startTimeMs + event.durationMs)

                val isImpact = event.semanticType == SemanticHapticType.COLLISION ||
                    event.semanticType == SemanticHapticType.HEAVY_IMPACT ||
                    event.intensity >= 0.70f

                val tickHeight = when {
                    isActive -> h
                    isImpact -> h
                    else -> (h * (0.50f + 0.45f * event.intensity.coerceIn(0f, 1f)))
                }

                val tickAlpha = when {
                    isActive -> 1.0f
                    isPast -> (0.40f + 0.50f * event.intensity).coerceIn(0.30f, 0.90f)
                    isImpact -> (0.50f + 0.50f * event.intensity).coerceIn(0.40f, 0.90f)
                    else -> (0.20f + 0.45f * event.intensity).coerceIn(0.20f, 0.70f)
                }

                val tickStrokeWidth = when {
                    isActive -> 2.25.dp.toPx()
                    isImpact -> 1.75.dp.toPx()
                    else -> 1.25.dp.toPx()
                }

                val tickColor = if (isActive) Color.White else colors.accentPrimary.copy(alpha = tickAlpha)

                drawLine(
                    color = tickColor,
                    start = Offset(markerX, (h - tickHeight) / 2f),
                    end = Offset(markerX, (h + tickHeight) / 2f),
                    strokeWidth = tickStrokeWidth
                )

                // Optional subtle secondary indicator for peakTimeMs if present and distinct
                val peakMs = event.peakTimeMs ?: (event.parameters["peakTimeMs"] as? Number)?.toLong()
                if (peakMs != null && peakMs != markerTimestampMs && peakMs in event.startTimeMs..(event.startTimeMs + event.durationMs)) {
                    val peakX = ((peakMs.toFloat() / totalDurationMs) * w).coerceIn(0f, w)
                    drawCircle(
                        color = (if (isActive) Color.White else colors.accentPrimary).copy(alpha = 0.5f),
                        radius = 1.dp.toPx(),
                        center = Offset(peakX, h / 2f)
                    )
                }

                // Highlight active pulse with glowing centroid dot
                if (isActive) {
                    drawCircle(
                        color = colors.accentPrimary,
                        radius = 2.dp.toPx(),
                        center = Offset(markerX, h / 2f)
                    )
                }
            }

            // 4. Authoritative playhead derived from player.currentPosition / playerDurationMs
            val playheadX = ((currentPositionMs.coerceIn(0L, durationMs).toFloat() / totalDurationMs) * w).coerceIn(0f, w)
            drawLine(
                color = Color.White,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, h),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

/**
 * Bento-Structured Research Debug HUD displaying real-time telemetry and haptic frequency status.
 */
@Composable
private fun ResearchDebugHudCard(
    uiState: VideoPlayerUiState,
    onTestHaptic: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val posMs = uiState.currentPositionMs
    val totalSec = posMs / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    val millis = posMs % 1000
    val formattedPosition = String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)

    val freqText = uiState.currentEventFrequencyHz?.let { String.format(Locale.US, "%.1f Hz", it) } ?: "---"
    val ampText = uiState.currentEventAmplitude?.let { String.format(Locale.US, "%.2f", it) } ?: "---"
    val durText = uiState.currentEventDurationMs?.let { "${it} ms" } ?: "---"

    val colors = HaptiXThemeTokens.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HaptiXShapeTokens.card)
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle, HaptiXShapeTokens.card)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESEARCH DIAGNOSTICS & TELEMETRY",
                    style = TechnicalMicroLabel,
                    color = colors.accentPrimary
                )

                Text(
                    text = "SAMPLING: 2000ms",
                    style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                    color = colors.textTertiary
                )
            }

            // Bento Group 1: Hardware Performance (CPU, GPU, Temp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(HaptiXShapeTokens.input)
                    .background(colors.controlSurface)
                    .border(0.5.dp, colors.borderSubtle, HaptiXShapeTokens.input)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "HARDWARE PERFORMANCE (2000ms CADENCE)",
                        style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                        color = colors.accentPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    uiState.latestMetric?.let { metric ->
                        DebugTelemetryRow(
                            label = "CPU Load",
                            value = metric.cpuUsagePercent?.let { "%.1f%%".format(it) } ?: "N/A"
                        )
                        DebugTelemetryRow(
                            label = "GPU Load",
                            value = metric.gpuUsagePercent?.let { "%.1f%%".format(it) } ?: "N/A"
                        )
                        DebugTelemetryRow(
                            label = "Device Temperature",
                            value = metric.deviceTemperatureCelsius?.let { "%.1f°C".format(it) } ?: "N/A"
                        )
                    } ?: run {
                        DebugTelemetryRow(label = "CPU Load", value = "Collecting...")
                        DebugTelemetryRow(label = "GPU Load", value = "N/A")
                        DebugTelemetryRow(label = "Device Temperature", value = "N/A")
                    }
                }
            }

            // Bento Group 2: Playback & Haptic Engine
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(HaptiXShapeTokens.input)
                    .background(colors.controlSurface)
                    .border(0.5.dp, colors.borderSubtle, HaptiXShapeTokens.input)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "SYNCHRONIZATION & ACTUATION ENGINE",
                        style = TechnicalMicroLabel.copy(fontSize = 10.sp),
                        color = colors.accentPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    DebugTelemetryRow(label = "Timeline Position", value = "${uiState.currentPositionMs} ms ($formattedPosition)")
                    DebugTelemetryRow(label = "Current Event Descriptor", value = uiState.currentHapticEvent ?: "None (idle)")
                    DebugTelemetryRow(label = "Actuation Frequency", value = freqText)
                    DebugTelemetryRow(label = "Actuation Amplitude", value = ampText)
                    DebugTelemetryRow(label = "Active Duration", value = durText)
                    DebugTelemetryRow(
                        label = "Actuator Capability Level",
                        value = uiState.capabilityLevel.name,
                        valueColor = when (uiState.capabilityLevel) {
                            HapticCapabilityLevel.SUPPORTED -> colors.signalGreen
                            HapticCapabilityLevel.LIMITED -> colors.warning
                            HapticCapabilityLevel.UNAVAILABLE -> colors.error
                        }
                    )
                }
            }

            // Actuation Trigger Button
            Button(
                onClick = onTestHaptic,
                colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                shape = HaptiXShapeTokens.input,
                border = BorderStroke(1.dp, colors.accentPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        this.contentDescription = "Test physical haptic vibration"
                    }
            ) {
                Text(
                    text = "TEST HAPTIC ACTUATION (MANUAL PULSE)",
                    style = TechnicalMicroLabel.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.accentPrimary
                )
            }
        }
    }
}

@Composable
private fun DebugTelemetryRow(
    label: String,
    value: String,
    valueColor: Color = HaptiXThemeTokens.colors.textPrimary
) {
    val colors = HaptiXThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TechnicalMicroLabel.copy(fontSize = 11.sp),
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = TechnicalMicroLabel.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = valueColor
        )
    }
}

/**
 * Developer Video Haptic Debug HUD Overlay (Section 16).
 */
@Composable
private fun HapticDebugOverlayCard(
    uiState: VideoPlayerUiState,
    onTestTimestamp: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HaptiXThemeTokens.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HaptiXShapeTokens.card)
            .background(colors.surface)
            .border(1.dp, colors.accentPrimary.copy(alpha = 0.5f), HaptiXShapeTokens.card)
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HAPTIC TIMELINE & MANUAL TRIGGERS",
                    style = TechnicalMicroLabel,
                    color = colors.accentPrimary
                )
                Text(
                    text = "STATUS: ${uiState.debugEventStatus}",
                    style = TechnicalMicroLabel.copy(fontWeight = FontWeight.Bold),
                    color = if (uiState.debugEventStatus == "TRIGGERED") colors.accentPrimary else colors.warning
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            val positionSec = String.format(java.util.Locale.US, "%02d:%06.3f", (uiState.currentPositionMs / 1000) / 60, (uiState.currentPositionMs % 60000) / 1000.0)
            DebugTelemetryRow(label = "VIDEO POSITION", value = positionSec, valueColor = colors.accentPrimary)
            DebugTelemetryRow(label = "Stimulus Title", value = uiState.debugVideoTitle)
            DebugTelemetryRow(label = "ACTIVE HAPTICS", value = uiState.debugEventName, valueColor = colors.accentPrimary)
            DebugTelemetryRow(label = "Active Window", value = uiState.debugEventTimeRange)
            DebugTelemetryRow(label = "NEXT HAPTIC EVENT", value = uiState.debugNextEventName)
            DebugTelemetryRow(label = "Next Window", value = uiState.debugNextEventTimeRange)
            DebugTelemetryRow(label = "SOURCE PARAMETERS", value = "freq: ${uiState.debugSourceFrequency} | amp: ${uiState.debugSourceAmplitude} | dur: ${uiState.debugSourceDuration}")
            DebugTelemetryRow(label = "LAST TRIGGERED EVENT", value = uiState.lastTriggeredEventDescription)
            DebugTelemetryRow(label = "ANDROID REPRESENTATION", value = uiState.debugAndroidRepresentation, valueColor = colors.signalGreen)
            DebugTelemetryRow(label = "ACTUATION STATE", value = uiState.debugActuationState, valueColor = if (uiState.debugActuationState == "TRIGGERED") colors.accentPrimary else colors.textSecondary)
            DebugTelemetryRow(label = "Haptic Events Loaded", value = "${uiState.hapticEventCount}")
            DebugTelemetryRow(label = "Tactile Output State", value = if (uiState.isHapticsEnabled) "ON" else "OFF", valueColor = if (uiState.isHapticsEnabled) colors.signalGreen else colors.error)
            DebugTelemetryRow(
                label = "Synchronization State",
                value = uiState.syncState,
                valueColor = if (uiState.syncState == "LOCKED") colors.accentPrimary else if (uiState.syncState == "SYNCED") colors.signalGreen else colors.warning
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "MANUAL TIMELINE POSITION TRIGGERS",
                style = TechnicalMicroLabel,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            val isF1 = uiState.debugVideoTitle.contains("F1", ignoreCase = true)
            if (isF1) {
                // V5 Key Timestamps — Row 1: Early events
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { onTestTimestamp(26540L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.borderSubtle),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("26.6s GEAR", fontSize = 9.sp, color = colors.accentPrimary)
                    }
                    Button(
                        onClick = { onTestTimestamp(30660L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.borderSubtle),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("30.7s ACCEL", fontSize = 9.sp, color = colors.accentPrimary)
                    }
                    Button(
                        onClick = { onTestTimestamp(42760L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.borderSubtle),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("42.8s CURB", fontSize = 9.sp, color = colors.accentPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // V5 Key Timestamps — Row 2: Mid events
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { onTestTimestamp(55520L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.borderSubtle),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("55.6s CORNER", fontSize = 9.sp, color = colors.accentPrimary)
                    }
                    Button(
                        onClick = { onTestTimestamp(73780L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.borderSubtle),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("73.8s CURB", fontSize = 9.sp, color = colors.accentPrimary)
                    }
                    Button(
                        onClick = { onTestTimestamp(80140L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.borderSubtle),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("80.2s FLYBY", fontSize = 9.sp, color = colors.accentPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // V5 Key Timestamps — Row 3: Crash + Cinematic
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { onTestTimestamp(105020L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.error.copy(alpha = 0.5f)),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("105.0s CRASH", fontSize = 9.sp, color = colors.error)
                    }
                    Button(
                        onClick = { onTestTimestamp(117420L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.accentSecondary.copy(alpha = 0.5f)),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("117.5s BASS", fontSize = 9.sp, color = colors.accentSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // V5 Key Timestamps — Row 4: Climax sequence
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { onTestTimestamp(123440L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.borderSubtle),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("123.5", fontSize = 9.sp, color = colors.warning)
                    }
                    Button(
                        onClick = { onTestTimestamp(123940L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.borderSubtle),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("124.0", fontSize = 9.sp, color = colors.warning)
                    }
                    Button(
                        onClick = { onTestTimestamp(124440L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.borderSubtle),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("124.5", fontSize = 9.sp, color = colors.warning)
                    }
                    Button(
                        onClick = { onTestTimestamp(124940L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.borderSubtle),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("125.0", fontSize = 9.sp, color = colors.warning)
                    }
                    Button(
                        onClick = { onTestTimestamp(125440L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.borderSubtle),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("125.5", fontSize = 9.sp, color = colors.warning)
                    }
                    Button(
                        onClick = { onTestTimestamp(125920L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.error.copy(alpha = 0.5f)),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("126.0", fontSize = 9.sp, color = colors.error)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { onTestTimestamp(12200L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.borderSubtle),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("12.2s JUMP", fontSize = 10.sp, color = colors.accentPrimary)
                    }
                    Button(
                        onClick = { onTestTimestamp(14500L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.borderSubtle),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("14.5s LAND", fontSize = 10.sp, color = colors.accentPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { onTestTimestamp(32500L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.borderSubtle),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("32.5s CLICK", fontSize = 10.sp, color = colors.accentPrimary)
                    }
                    Button(
                        onClick = { onTestTimestamp(39000L) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.focusSurface),
                        border = BorderStroke(0.5.dp, colors.error.copy(alpha = 0.5f)),
                        shape = HaptiXShapeTokens.micro
                    ) {
                        Text("39.0s SENTINEL", fontSize = 10.sp, color = colors.error)
                    }
                }
            }
        }
    }
}
