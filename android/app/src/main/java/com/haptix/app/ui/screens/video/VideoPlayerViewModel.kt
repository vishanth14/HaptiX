package com.haptix.app.ui.screens.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticPattern
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
    val controlsVisible: Boolean = true,
    val latestMetric: PerformanceMetric? = null,
    // Research Debug Telemetry
    val isDebugMode: Boolean = false,
    val isHapticDebugOverlayVisible: Boolean = false,
    val hapticSource: String = "UNKNOWN",
    val hapticEventCount: Int = 0,
    val hapticEvents: List<HapticEvent> = emptyList(),
    val hapticPattern: HapticPattern? = null,
    val currentHapticEvent: String? = null,
    val lastTriggeredEventDescription: String = "None",
    val currentEventDescription: String? = null,
    val currentEventFrequencyHz: Float? = null,
    val currentEventAmplitude: Float? = null,
    val currentEventDurationMs: Long? = null,
    // V4.3 Video Haptic Debug Overlay Fields
    val debugVideoTitle: String = "F1",
    val debugEventName: String = "IDLE",
    val debugEventTimeRange: String = "None",
    val debugEventStatus: String = "NOT_TRIGGERED",
    val debugLastTriggerSec: String = "None",
    val debugLastVibrationSec: String = "None",
    // Section 11 Research Diagnostic Mode Fields
    val debugNextEventName: String = "None",
    val debugNextEventTimeRange: String = "None",
    val debugSourceFrequency: String = "N/A",
    val debugSourceAmplitude: String = "N/A",
    val debugSourceDuration: String = "N/A",
    val debugAndroidRepresentation: String = "IDLE",
    val debugActuationState: String = "IDLE",
    // Error Handling fields
    val errorMessage: String? = null,
    val hapticErrorMessage: String? = null,
    val isHapticLoaded: Boolean = true,
    val syncState: String = "SYNCED"
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

    init {
        scope.launch {
            performanceMonitor.currentMetrics.collect { metric ->
                if (metric != null) {
                    _uiState.value = _uiState.value.copy(latestMetric = metric)
                }
            }
        }
    }

    private var playbackJob: Job? = null
    private var hasExternalTimelineUpdates: Boolean = false
    private var loadedVideoId: String? = null
    private var savedPositionMs: Long = 0L

    /**
     * Initializes playback session with video metadata and haptic synchronization config.
     */
    fun loadVideo(videoId: String, sessionHapticsEnabled: Boolean = true) {
        if (loadedVideoId == videoId && _uiState.value.videoItem != null) return
        loadedVideoId = videoId

        // Invalidate previous playback generation, cancel jobs, and reset synchronizer
        playbackJob?.cancel()
        playbackJob = null
        hapticSynchronizer?.reset()
        savedPositionMs = 0L

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

            // Load haptic pattern: if remote hapticUrl is specified, retrieve over network, else local assets
            val patternResult = if (video != null && video.hapticUrl.isNotBlank()) {
                hapticRepository.loadHapticPatternFromUrl(video.hapticUrl, video.id)
            } else if (video != null) {
                hapticRepository.loadPatternForVideoItem(video)
            } else {
                hapticRepository.loadHapticPatternForVideo(videoId)
            }

            val pattern = if (patternResult.isSuccess) {
                val p = patternResult.getOrThrow()
                hapticSynchronizer?.setPattern(p)
                p
            } else {
                val ex = patternResult.exceptionOrNull()
                com.haptix.app.util.HaptiXLog.e("Haptic stream load error for $videoId: ${ex?.message}")
                hapticSynchronizer?.setPattern(com.haptix.app.data.model.HapticPattern(videoId = videoId, events = emptyList()))
                hapticSynchronizer?.isHapticsEnabled = false
                null
            }

            val isHapticSuccess = patternResult.isSuccess
            val hapticErrMsg = if (patternResult.isFailure) {
                patternResult.exceptionOrNull()?.message ?: "Failed to load haptic stream"
            } else null

            val activeSource = hapticRepository.getActiveSource()
            val events = pattern?.events ?: emptyList()
            val eventCount = events.size
            val firstEventTs = events.firstOrNull()?.startTimeMs
            val lastEventTs = events.lastOrNull()?.startTimeMs
            val totalPatternDuration = events.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L
            val freqPointsCount = events.count { it.frequencyHz != null || it.parameters.containsKey("frequencyHz") }
            val sourceName = if (video?.hapticUrl?.isNotBlank() == true) {
                "REMOTE_URL (${video.hapticUrl})"
            } else {
                "${activeSource.name} (${pattern?.source ?: "assets/haptics/${videoId}_haptic_representation.json"})"
            }

            performanceMonitor.startMonitoring(videoId) { _uiState.value.currentPositionMs }
            performanceMonitor.pauseMonitoring()

            com.haptix.app.util.HaptiXLog.d("HAPTICS_ENABLED=$effectiveHaptics (video=$videoId)")

            _uiState.value = _uiState.value.copy(
                videoItem = video,
                durationMs = duration,
                currentPositionMs = savedPositionMs,
                isPlaying = false,
                isCompleted = false,
                isHapticSupported = isHardwareSupported,
                capabilityLevel = capLevel,
                isHapticsEnabled = if (!isHapticSuccess && video?.hapticUrl?.isNotBlank() == true) false else effectiveHaptics,
                controlsVisible = true,
                hapticSource = sourceName,
                hapticEventCount = eventCount,
                hapticEvents = events,
                hapticPattern = pattern,
                hapticErrorMessage = hapticErrMsg,
                isHapticLoaded = isHapticSuccess || video?.hapticUrl.isNullOrBlank(),
                errorMessage = if (video == null) "Stimulus not found: $videoId" else null,
                debugVideoTitle = if (videoId.contains("f1", ignoreCase = true)) "F1" else if (videoId.contains("koji", ignoreCase = true)) "Koji" else video?.title ?: videoId
            )

            com.haptix.app.util.HaptiXLog.d(
                """
                === VIDEO & HAPTIC ASSET RUNTIME VERIFICATION ===
                Video ID: $videoId
                Haptics enabled: $effectiveHaptics
                Asset loaded: ${pattern != null}
                JSON parsing: ${if (patternResult.isSuccess) "SUCCESS" else "FAILURE (${patternResult.exceptionOrNull()?.message})"}
                Loaded events: $eventCount
                First event timestamp: ${firstEventTs?.let { "${it}ms" } ?: "N/A"}
                Last event timestamp: ${lastEventTs?.let { "${it}ms" } ?: "N/A"}
                Total pattern duration: ${totalPatternDuration}ms
                Active haptic source: ${activeSource.name}
                =================================================
                """.trimIndent()
            )
        }
    }

    /**
     * Development-only manual tactile verification trigger.
     * Actuates a controlled smooth envelope through the active haptic engine.
     */
    fun testHapticActuation() {
        val player = hapticEngine as? AndroidHapticPlayer
        if (player != null) {
            player.playTestHaptic()
        } else {
            val testEvent = com.haptix.app.data.model.HapticEvent(
                startTimeMs = 0L,
                durationMs = 600L,
                intensity = 0.85f,
                sharpness = 0.60f,
                semanticType = com.haptix.app.data.model.SemanticHapticType.SMOOTH,
                parameters = mapOf(
                    "description" to "Manual Test Haptic",
                    "attackMs" to 150L,
                    "sustainMs" to 250L,
                    "releaseMs" to 200L,
                    "frequencyHz" to 180.0f
                )
            )
            hapticEngine?.playEvent(testEvent)
        }
    }

    /**
     * Invokes the actual event associated with a specific timestamp.
     */
    fun testEventAtTimestamp(targetMs: Long) {
        val pattern = hapticSynchronizer?.getPattern()
        val event = pattern?.events?.minByOrNull { kotlin.math.abs(it.startTimeMs - targetMs) }
        if (event != null && kotlin.math.abs(event.startTimeMs - targetMs) < 1200L) {
            com.haptix.app.util.HaptiXLog.d("MANUAL_TIMESTAMP_TRIGGER: target=${targetMs}ms, event=${event.id} (${event.semanticType}) at ${event.startTimeMs}ms")
            hapticEngine?.playEvent(event)
        } else {
            com.haptix.app.util.HaptiXLog.w("MANUAL_TIMESTAMP_TRIGGER_FAILED: no event near ${targetMs}ms (pattern size=${pattern?.events?.size})")
        }
    }

    /**
     * Updates playback state reported by ExoPlayer.
     * Guarantees that buffering safely pauses active actuation without advancing the timeline.
     */
    fun updatePlaybackState(
        isPlaying: Boolean,
        isCompleted: Boolean,
        isBuffering: Boolean = false,
        currentPositionMs: Long? = null
    ) {
        val actualPos = currentPositionMs ?: _uiState.value.currentPositionMs
        val wasPlaying = _uiState.value.isPlaying
        val wasBuffering = _uiState.value.syncState == "BUFFERING"

        if (isPlaying || currentPositionMs != null) {
            hasExternalTimelineUpdates = true
            playbackJob?.cancel()
            playbackJob = null
        }

        val syncState = when {
            !_uiState.value.isHapticsEnabled -> "HAPTICS OFF"
            isBuffering -> "BUFFERING"
            isCompleted -> "WAITING"
            !isPlaying -> "PAUSED"
            hapticSynchronizer?.isActuatingState == true -> "LOCKED"
            else -> "SYNCED"
        }

        _uiState.value = _uiState.value.copy(
            isPlaying = isPlaying,
            isCompleted = isCompleted,
            currentPositionMs = actualPos,
            syncState = syncState
        )

        if (isBuffering) {
            hapticSynchronizer?.onBuffering(actualPos)
            performanceMonitor.pauseMonitoring()
        } else if (isCompleted) {
            com.haptix.app.util.HaptiXLog.d("Video playback completed")
            hapticSynchronizer?.onPlaybackComplete()
            performanceMonitor.stopMonitoring()
        } else if (isPlaying && (!wasPlaying || wasBuffering)) {
            com.haptix.app.util.HaptiXLog.d("Video playback started/resumed at ${actualPos}ms")
            hapticSynchronizer?.onResume(actualPos)
            performanceMonitor.resumeMonitoring()
        } else if (!isPlaying && wasPlaying) {
            com.haptix.app.util.HaptiXLog.d("Video paused (user/system)")
            hapticSynchronizer?.onPause("USER_OR_SYSTEM_PAUSE", actualPos)
            performanceMonitor.pauseMonitoring()
        }
    }

    /**
     * Handles position discontinuities from ExoPlayer (e.g. user seeks, media controller seeks, auto-loops).
     */
    fun onPlayerPositionDiscontinuity(oldPositionMs: Long, newPositionMs: Long, reason: Int) {
        seekTo(newPositionMs)
    }

    /**
     * Reports an unrecoverable video stream or decoding error (e.g. invalid URL, network failure).
     */
    fun onPlaybackError(message: String) {
        com.haptix.app.util.HaptiXLog.e("VIDEO_PLAYBACK_ERROR: $message")
        _uiState.value = _uiState.value.copy(
            isPlaying = false,
            errorMessage = message
        )
        hapticSynchronizer?.onPause("PLAYBACK_ERROR")
        performanceMonitor.pauseMonitoring()
    }

    /**
     * Updates real duration from ExoPlayer media metadata and audits against haptic duration.
     */
    fun updateDuration(durationMs: Long) {
        if (durationMs > 0L) {
            val pattern = hapticSynchronizer?.getPattern()
            val patternDurationMs = pattern?.videoDurationMs
                ?: pattern?.events?.maxOfOrNull { it.startTimeMs + it.durationMs }
                ?: 0L
            com.haptix.app.util.HaptiXLog.d(
                """
                === VIDEO DURATION CHECK ===
                VIDEO_DURATION_MS=$durationMs
                HAPTIC_PATTERN_DURATION_MS=$patternDurationMs
                diffMs=${kotlin.math.abs(durationMs - patternDurationMs)}
                ============================
                """.trimIndent()
            )
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

        com.haptix.app.util.HaptiXLog.d("Video playback started at ${_uiState.value.currentPositionMs}ms")
        _uiState.value = _uiState.value.copy(isPlaying = true, isCompleted = false)
        hapticSynchronizer?.onResume(_uiState.value.currentPositionMs)
        performanceMonitor.resumeMonitoring()

        // Fallback simulated ticker for unit testing environments where ExoPlayer is not attached
        if (!hasExternalTimelineUpdates) {
            playbackJob?.cancel()
            playbackJob = scope.launch {
                val tickIntervalMs = 50L
                while (isActive && _uiState.value.isPlaying) {
                    delay(tickIntervalMs)
                    if (hasExternalTimelineUpdates) break
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
                        performanceMonitor.stopMonitoring()
                        break
                    } else {
                        onTimelinePositionChanged(current + tickIntervalMs)
                    }
                }
            }
        }
    }

    fun pause() {
        playbackJob?.cancel()
        playbackJob = null
        com.haptix.app.util.HaptiXLog.d("Video paused")
        _uiState.value = _uiState.value.copy(isPlaying = false)
        hapticSynchronizer?.onPause("USER_OR_SYSTEM_PAUSE", _uiState.value.currentPositionMs)
        performanceMonitor.pauseMonitoring()
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val oldPos = _uiState.value.currentPositionMs
        val duration = _uiState.value.durationMs.coerceAtLeast(0L)
        val clamped = if (duration > 0L) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        savedPositionMs = clamped
        com.haptix.app.util.HaptiXLog.d("Video seek: ${oldPos}ms → ${clamped}ms")
        _uiState.value = _uiState.value.copy(
            currentPositionMs = clamped,
            isCompleted = duration > 0L && clamped >= duration
        )
        hapticSynchronizer?.onSeek(clamped)
    }

    fun fastForward(deltaMs: Long = 10_000L) {
        val current = _uiState.value.currentPositionMs
        val duration = _uiState.value.durationMs.coerceAtLeast(0L)
        val target = if (duration > 0L) (current + deltaMs).coerceIn(0L, duration) else (current + deltaMs).coerceAtLeast(0L)
        seekTo(target)
    }

    fun rewind(deltaMs: Long = 10_000L) {
        val current = _uiState.value.currentPositionMs
        val duration = _uiState.value.durationMs.coerceAtLeast(0L)
        val target = if (duration > 0L) (current - deltaMs).coerceIn(0L, duration) else (current - deltaMs).coerceAtLeast(0L)
        seekTo(target)
    }

    fun toggleVolume() {
        _uiState.value = _uiState.value.copy(isMuted = !_uiState.value.isMuted)
    }

    fun toggleFullscreen() {
        _uiState.value = _uiState.value.copy(isFullscreen = !_uiState.value.isFullscreen)
    }

    fun setControlsVisibility(visible: Boolean) {
        if (_uiState.value.controlsVisible != visible) {
            _uiState.value = _uiState.value.copy(controlsVisible = visible)
        }
    }

    fun toggleControlsVisibility() {
        val next = !_uiState.value.controlsVisible
        _uiState.value = _uiState.value.copy(controlsVisible = next)
    }

    fun toggleDebugMode() {
        _uiState.value = _uiState.value.copy(isDebugMode = !_uiState.value.isDebugMode)
    }

    fun toggleHapticDebugOverlay() {
        _uiState.value = _uiState.value.copy(isHapticDebugOverlayVisible = !_uiState.value.isHapticDebugOverlayVisible)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        if (!_uiState.value.isHapticSupported || _uiState.value.capabilityLevel == HapticCapabilityLevel.UNAVAILABLE) return
        _uiState.value = _uiState.value.copy(isHapticsEnabled = enabled)
        hapticSynchronizer?.isHapticsEnabled = enabled
        if (enabled) {
            hapticSynchronizer?.alignEventStatesToPosition(_uiState.value.currentPositionMs)
        }
    }

    fun toggleHaptics() {
        if (!_uiState.value.isHapticSupported || _uiState.value.capabilityLevel == HapticCapabilityLevel.UNAVAILABLE) return
        val next = !_uiState.value.isHapticsEnabled
        setHapticsEnabled(next)
    }

    /**
     * Millisecond-precision timeline update dispatched directly to [HapticSynchronizer].
     */
    fun onTimelinePositionChanged(positionMs: Long, isFromExternalPlayer: Boolean = false) {
        if (isFromExternalPlayer) {
            hasExternalTimelineUpdates = true
            playbackJob?.cancel()
            playbackJob = null
        }
        val duration = _uiState.value.durationMs.coerceAtLeast(0L)
        val clampedPos = if (duration > 0L) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        savedPositionMs = clampedPos
        hapticSynchronizer?.onTimelineUpdate(clampedPos)

        // Query synchronizer for latest event debug info
        val lastEvent = hapticSynchronizer?.getLastDispatched()
        val isEventActive = lastEvent != null && positionMs in lastEvent.startTimeMs..(lastEvent.endTimeMs + 100L)

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
        val currentEventStr = if (isEventActive) {
            "${lastEvent?.startTimeMs}ms: ${lastEvent?.semanticType} (dur=${lastEvent?.durationMs}ms, amp=${String.format(java.util.Locale.US, "%.2f", amp ?: 0f)})"
        } else "None (idle)"

        val lastTriggeredStr = if (lastEvent != null) {
            "${lastEvent.startTimeMs}ms: ${lastEvent.semanticType} (${lastEvent.durationMs}ms)"
        } else "None"

        val lastTriggerSec = if (lastEvent != null) {
            String.format(java.util.Locale.US, "%.2fs", lastEvent.startTimeMs / 1000.0)
        } else "None"

        val lastVibrationSec = if (lastEvent != null) {
            val lastTs = hapticSynchronizer?.getLastDispatchedTimestampMs() ?: lastEvent.startTimeMs
            String.format(java.util.Locale.US, "%.2fs", lastTs / 1000.0)
        } else "None"

        val eventTimeRange = if (lastEvent != null) {
            String.format(
                java.util.Locale.US,
                "%.2fs → %.2fs",
                lastEvent.startTimeMs / 1000.0,
                (lastEvent.startTimeMs + lastEvent.durationMs) / 1000.0
            )
        } else "None"

        val activeEvents = hapticSynchronizer?.getActiveEvents(positionMs) ?: emptyList()
        val nextEvent = hapticSynchronizer?.getNextEvent(positionMs)
        val activeEvent = activeEvents.firstOrNull() ?: if (isEventActive) lastEvent else null

        val sourceFreqStr = activeEvent?.frequencyHz?.let { String.format(java.util.Locale.US, "%.1f Hz", it) }
            ?: (activeEvent?.parameters?.get("frequencyHz") as? Number)?.let { String.format(java.util.Locale.US, "%.1f Hz", it.toFloat()) }
            ?: "N/A"

        val sourceAmpStr = activeEvent?.let { ev ->
            val a = (ev.parameters["amplitude"] as? Number)?.toFloat()
                ?: (ev.parameters["intensity"] as? Number)?.toFloat()
                ?: ev.intensity
            String.format(java.util.Locale.US, "%.3f", a)
        } ?: "N/A"

        val sourceDurStr = activeEvent?.let { "${it.durationMs} ms" } ?: "N/A"

        val nextEventName = if (nextEvent != null) {
            val idx = hapticSynchronizer?.getEventIndex(nextEvent) ?: -1
            "Event #${idx.takeIf { it >= 0 }?.toString() ?: "?"} (${nextEvent.semanticType.name})"
        } else "None"

        val nextEventRange = if (nextEvent != null) {
            String.format(
                java.util.Locale.US,
                "%.3fs → %.3fs (in %dms)",
                nextEvent.startTimeMs / 1000.0,
                (nextEvent.startTimeMs + nextEvent.durationMs) / 1000.0,
                nextEvent.startTimeMs - positionMs
            )
        } else "None"

        val androidPlayer = hapticEngine as? AndroidHapticPlayer
        val androidRep = if (activeEvent != null) {
            androidPlayer?.lastDispatchedRepresentation ?: androidPlayer?.getExpectedRepresentation() ?: "IDLE"
        } else if (lastEvent != null) {
            androidPlayer?.lastDispatchedRepresentation ?: "IDLE"
        } else "IDLE"

        val actuationState = if (isEventActive || activeEvents.isNotEmpty()) "TRIGGERED" else if (lastEvent != null) "COMPLETED" else "IDLE"
        val eventStatus = if (isEventActive || activeEvents.isNotEmpty()) "TRIGGERED" else if (lastEvent != null) "COMPLETED" else "IDLE"
        val syncState = hapticSynchronizer?.getSyncState() ?: "SYNCED"

        _uiState.value = _uiState.value.copy(
            currentPositionMs = positionMs,
            currentEventFrequencyHz = freq,
            currentEventAmplitude = amp,
            currentEventDurationMs = dur,
            currentEventDescription = desc,
            currentHapticEvent = currentEventStr,
            lastTriggeredEventDescription = lastTriggeredStr,
            debugEventName = if (activeEvent != null) {
                val idx = hapticSynchronizer?.getEventIndex(activeEvent) ?: -1
                "Event #${idx.takeIf { it >= 0 }?.toString() ?: "?"} (${activeEvent.semanticType.name})"
            } else if (lastEvent != null) lastEvent.semanticType.name else "IDLE",
            debugEventTimeRange = if (activeEvent != null) {
                String.format(
                    java.util.Locale.US,
                    "%.3fs → %.3fs",
                    activeEvent.startTimeMs / 1000.0,
                    (activeEvent.startTimeMs + activeEvent.durationMs) / 1000.0
                )
            } else eventTimeRange,
            debugEventStatus = eventStatus,
            debugLastTriggerSec = lastTriggerSec,
            debugLastVibrationSec = lastVibrationSec,
            debugNextEventName = nextEventName,
            debugNextEventTimeRange = nextEventRange,
            debugSourceFrequency = sourceFreqStr,
            debugSourceAmplitude = sourceAmpStr,
            debugSourceDuration = sourceDurStr,
            debugAndroidRepresentation = androidRep,
            debugActuationState = actuationState,
            syncState = syncState
        )

    }

    /**
     * Returns all performance metrics recorded during this playback session.
     */
    fun getRecordedPerformanceMetrics(): List<PerformanceMetric> = performanceMonitor.getRecordedMetrics()

    /**
     * Alias for [getRecordedPerformanceMetrics].
     */
    fun getCollectedPerformanceMetrics(): List<PerformanceMetric> = performanceMonitor.getRecordedMetrics()

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        hapticSynchronizer?.reset()
        performanceMonitor.stopMonitoring()
    }
}
