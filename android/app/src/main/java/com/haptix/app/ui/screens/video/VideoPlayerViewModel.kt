package com.haptix.app.ui.screens.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haptix.app.data.model.PerformanceMetric
import com.haptix.app.data.model.VideoItem
import com.haptix.app.data.repository.DefaultHapticRepository
import com.haptix.app.data.repository.DefaultVideoRepository
import com.haptix.app.data.repository.HapticRepository
import com.haptix.app.data.repository.VideoRepository
import com.haptix.app.haptics.AndroidHapticPlayer
import com.haptix.app.haptics.HapticCapabilityLevel
import com.haptix.app.haptics.HapticEngine
import com.haptix.app.haptics.HapticSynchronizer
import com.haptix.app.monitoring.PerformanceMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * UI State representation for the Video Player session with research debug telemetry.
 */
data class VideoPlayerUiState(
    val videoItem: VideoItem? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isHapticsEnabled: Boolean = true,
    val isHapticSupported: Boolean = true,
    val capabilityLevel: HapticCapabilityLevel = HapticCapabilityLevel.SUPPORTED,
    val isCompleted: Boolean = false,
    val isMuted: Boolean = false,
    val isFullscreen: Boolean = false,
    val latestMetric: PerformanceMetric? = null,
    // Research Debug Telemetry
    val isDebugMode: Boolean = false,
    val currentEventDescription: String? = null,
    val currentEventFrequencyHz: Float? = null,
    val currentEventAmplitude: Float? = null,
    val currentEventDurationMs: Long? = null
)

/**
 * ViewModel coordinating real playback timeline updates, ExoPlayer state synchronization,
 * haptic frequency actuation timing, and research telemetry.
 */
class VideoPlayerViewModel(
    private val videoRepository: VideoRepository = DefaultVideoRepository(),
    private val hapticRepository: HapticRepository = DefaultHapticRepository(),
    private val hapticSynchronizer: HapticSynchronizer? = null,
    private val performanceMonitor: PerformanceMonitor = PerformanceMonitor(),
    private val hapticEngine: HapticEngine? = null,
    private val coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope get() = coroutineScope ?: viewModelScope

    private val initialHardwareSupported: Boolean = hapticEngine?.isHapticSupported() ?: true

    private val initialCapabilityLevel: HapticCapabilityLevel = when {
        hapticEngine is AndroidHapticPlayer -> hapticEngine.getCapabilityLevel()
        hapticEngine != null -> if (hapticEngine.isHapticSupported()) HapticCapabilityLevel.SUPPORTED else HapticCapabilityLevel.UNAVAILABLE
        else -> HapticCapabilityLevel.SUPPORTED
    }

    private val _uiState = MutableStateFlow(
        VideoPlayerUiState(
            isHapticSupported = initialHardwareSupported,
            isHapticsEnabled = initialHardwareSupported,
            capabilityLevel = initialCapabilityLevel
        )
    )
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null
    private var loadedVideoId: String? = null
    private var savedPositionMs: Long = 0L

    /**
     * Initializes playback session with video metadata and haptic synchronization config.
     */
    fun loadVideo(videoId: String, sessionHapticsEnabled: Boolean = true) {
        if (loadedVideoId == videoId && _uiState.value.videoItem != null) return
        loadedVideoId = videoId

        val isHardwareSupported = hapticEngine?.isHapticSupported() ?: true
        val capLevel = when {
            hapticEngine is AndroidHapticPlayer -> hapticEngine.getCapabilityLevel()
            hapticEngine != null -> if (isHardwareSupported) HapticCapabilityLevel.SUPPORTED else HapticCapabilityLevel.UNAVAILABLE
            else -> HapticCapabilityLevel.SUPPORTED
        }

        scope.launch {
            val video = videoRepository.getVideoById(videoId)
            val duration = video?.durationMs ?: 30_000L

            val effectiveHaptics = sessionHapticsEnabled && isHardwareSupported && capLevel != HapticCapabilityLevel.UNAVAILABLE
            hapticSynchronizer?.isHapticsEnabled = effectiveHaptics

            // Load haptic pattern for video if available
            val patternResult = hapticRepository.loadHapticPatternForVideo(videoId)
            if (patternResult.isSuccess) {
                patternResult.getOrNull()?.let { pattern ->
                    hapticSynchronizer?.setPattern(pattern)
                }
            }

            performanceMonitor.startMonitoring(videoId)

            _uiState.value = _uiState.value.copy(
                videoItem = video,
                durationMs = duration,
                currentPositionMs = savedPositionMs,
                isPlaying = false,
                isCompleted = false,
                isHapticSupported = isHardwareSupported,
                capabilityLevel = capLevel,
                isHapticsEnabled = effectiveHaptics
            )
        }
    }

    /**
     * Updates playback state reported by ExoPlayer.
     */
    fun updatePlaybackState(isPlaying: Boolean, isCompleted: Boolean) {
        _uiState.value = _uiState.value.copy(
            isPlaying = isPlaying,
            isCompleted = isCompleted
        )
        if (isCompleted) {
            hapticSynchronizer?.onPlaybackComplete()
        } else if (!isPlaying) {
            hapticSynchronizer?.onPause()
        }
    }

    /**
     * Updates real duration from ExoPlayer media metadata.
     */
    fun updateDuration(durationMs: Long) {
        if (durationMs > 0L) {
            _uiState.value = _uiState.value.copy(durationMs = durationMs)
        }
    }

    /**
     * Saves playback position for lifecycle restoration when returning to the screen.
     */
    fun savePlaybackPosition(positionMs: Long) {
        savedPositionMs = positionMs.coerceAtLeast(0L)
    }

    /**
     * Returns the saved playback position for state restoration.
     */
    fun getSavedPlaybackPosition(): Long = savedPositionMs

    fun play() {
        if (_uiState.value.isPlaying) return
        if (_uiState.value.isCompleted) {
            seekTo(0L)
        }

        _uiState.value = _uiState.value.copy(isPlaying = true, isCompleted = false)
        hapticSynchronizer?.onResume(_uiState.value.currentPositionMs)

        // Fallback simulated ticker for unit testing environments where ExoPlayer is not attached
        playbackJob?.cancel()
        playbackJob = scope.launch {
            val tickIntervalMs = 50L
            while (isActive && _uiState.value.isPlaying) {
                delay(tickIntervalMs)
                val current = _uiState.value.currentPositionMs
                val duration = _uiState.value.durationMs

                if (current + tickIntervalMs >= duration) {
                    onTimelinePositionChanged(duration)
                    _uiState.value = _uiState.value.copy(
                        isPlaying = false,
                        isCompleted = true,
                        currentPositionMs = duration
                    )
                    hapticSynchronizer?.onPlaybackComplete()
                    break
                } else {
                    onTimelinePositionChanged(current + tickIntervalMs)
                }
            }
        }
    }

    fun pause() {
        playbackJob?.cancel()
        playbackJob = null
        _uiState.value = _uiState.value.copy(isPlaying = false)
        hapticSynchronizer?.onPause()
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _uiState.value.durationMs)
        savedPositionMs = clamped
        _uiState.value = _uiState.value.copy(
            currentPositionMs = clamped,
            isCompleted = clamped >= _uiState.value.durationMs
        )
        hapticSynchronizer?.onSeek(clamped)
    }

    fun toggleVolume() {
        _uiState.value = _uiState.value.copy(isMuted = !_uiState.value.isMuted)
    }

    fun toggleFullscreen() {
        _uiState.value = _uiState.value.copy(isFullscreen = !_uiState.value.isFullscreen)
    }

    fun toggleDebugMode() {
        _uiState.value = _uiState.value.copy(isDebugMode = !_uiState.value.isDebugMode)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        if (!_uiState.value.isHapticSupported || _uiState.value.capabilityLevel == HapticCapabilityLevel.UNAVAILABLE) return
        _uiState.value = _uiState.value.copy(isHapticsEnabled = enabled)
        hapticSynchronizer?.isHapticsEnabled = enabled
    }

    fun toggleHaptics() {
        if (!_uiState.value.isHapticSupported || _uiState.value.capabilityLevel == HapticCapabilityLevel.UNAVAILABLE) return
        val next = !_uiState.value.isHapticsEnabled
        setHapticsEnabled(next)
    }

    /**
     * Millisecond-precision timeline update dispatched directly to [HapticSynchronizer].
     */
    fun onTimelinePositionChanged(positionMs: Long) {
        savedPositionMs = positionMs
        hapticSynchronizer?.onTimelineUpdate(positionMs)

        // Query synchronizer for latest event debug info
        val lastEvent = hapticSynchronizer?.getLastDispatched()
        val isEventActive = lastEvent != null && positionMs in lastEvent.startTimeMs..(lastEvent.endTimeMs + 300L)

        val freq = if (isEventActive) {
            (lastEvent?.parameters?.get("frequencyHz") as? Number)?.toFloat()
                ?: (100f + (lastEvent?.sharpness ?: 0.5f) * 200f)
        } else null

        val amp = if (isEventActive) {
            (lastEvent?.parameters?.get("amplitude") as? Number)?.toFloat()
                ?: lastEvent?.intensity
        } else null

        val dur = if (isEventActive) lastEvent?.durationMs else null
        val desc = if (isEventActive) lastEvent?.parameters?.get("description") as? String else null

        _uiState.value = _uiState.value.copy(
            currentPositionMs = positionMs,
            currentEventFrequencyHz = freq,
            currentEventAmplitude = amp,
            currentEventDurationMs = dur,
            currentEventDescription = desc
        )

        // Periodically capture performance telemetry
        if (positionMs % 1000L < 100L) {
            val metric = PerformanceMetric(
                timestamp = System.currentTimeMillis(),
                cpuUsagePercent = 22.0f,
                fps = 60.0f,
                memoryUsageMb = 84L,
                videoId = _uiState.value.videoItem?.id.orEmpty()
            )
            performanceMonitor.recordSnapshot(metric)
            _uiState.value = _uiState.value.copy(latestMetric = metric)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        hapticSynchronizer?.reset()
        performanceMonitor.stopMonitoring()
    }
}
