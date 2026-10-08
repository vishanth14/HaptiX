package com.haptix.app.monitoring

import android.content.Context
import com.haptix.app.data.model.PerformanceMetric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Coordinates asynchronous, background telemetry collection of device hardware performance metrics
 * (CPU utilization, GPU utilization, and device thermal temperature) every 2,000 milliseconds
 * during active experimental video playback.
 *
 * Architecture & Design Contract:
 * - Strictly decoupled from [com.haptix.app.haptics.HapticSynchronizer], [com.haptix.app.haptics.AndroidHapticPlayer],
 *   and ExoPlayer internals.
 * - Does not block the main/UI thread.
 * - Samples only during active playback; paused when video pauses, stopped when video ends.
 * - Stores all collected samples in an in-memory session buffer ready for later aggregation.
 */
class PerformanceMonitor(
    context: Context? = null,
    private val cpuCollector: CpuCollector = AndroidCpuCollector(),
    private val gpuCollector: GpuCollector = AndroidGpuCollector(),
    private val temperatureCollector: TemperatureCollector = AndroidTemperatureCollector(context),
    private val coroutineScope: CoroutineScope? = null,
    val samplingIntervalMs: Long = 2000L
) {
    private val internalScope = CoroutineScope(Dispatchers.Default)
    private val scope get() = coroutineScope ?: internalScope

    private val _currentMetrics = MutableStateFlow<PerformanceMetric?>(null)
    val currentMetrics: StateFlow<PerformanceMetric?> = _currentMetrics.asStateFlow()

    private val sessionMetrics = CopyOnWriteArrayList<PerformanceMetric>()

    private var samplingJob: Job? = null
    private var activeVideoId: String = ""
    private var playbackPositionSupplier: (() -> Long)? = null

    @Volatile
    var isMonitoring: Boolean = false
        private set

    @Volatile
    var isPaused: Boolean = false
        private set

    /**
     * Initiates metric recording for the given video playback session.
     * Starts the 2,000ms periodic sampling loop on a background thread.
     *
     * @param videoId Associated stimulus identifier.
     * @param playbackPositionProvider Optional supplier returning current video playback position in ms.
     */
    fun startMonitoring(
        videoId: String,
        playbackPositionProvider: (() -> Long)? = null
    ) {
        this.activeVideoId = videoId
        this.playbackPositionSupplier = playbackPositionProvider
        sessionMetrics.clear()
        isMonitoring = true
        isPaused = false
        startSamplingLoop()
    }

    /**
     * Backward-compatible alias for [startMonitoring].
     */
    fun start(videoId: String, playbackPositionProvider: (() -> Long)? = null) {
        startMonitoring(videoId, playbackPositionProvider)
    }

    /**
     * Pauses the sampling loop during video pause, buffering, or seeking.
     * Does not clear accumulated session metrics.
     */
    fun pauseMonitoring() {
        isPaused = true
    }

    /**
     * Alias for [pauseMonitoring].
     */
    fun pause() {
        pauseMonitoring()
    }

    /**
     * Resumes the sampling loop when video playback continues.
     */
    fun resumeMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true
        }
        isPaused = false
        if (samplingJob == null || samplingJob?.isActive != true) {
            startSamplingLoop()
        }
    }

    /**
     * Alias for [resumeMonitoring].
     */
    fun resume() {
        resumeMonitoring()
    }

    /**
     * Stops active metric recording and returns aggregated session metrics.
     */
    fun stopMonitoring(): List<PerformanceMetric> {
        isMonitoring = false
        isPaused = false
        samplingJob?.cancel()
        samplingJob = null
        return sessionMetrics.toList()
    }

    /**
     * Alias for [stopMonitoring].
     */
    fun stop(): List<PerformanceMetric> {
        return stopMonitoring()
    }

    /**
     * Returns a snapshot copy of all metrics collected during this session.
     */
    fun getRecordedMetrics(): List<PerformanceMetric> {
        return sessionMetrics.toList()
    }

    /**
     * Records an externally supplied performance snapshot (e.g. for testing or explicit checkpoints).
     */
    fun recordSnapshot(metric: PerformanceMetric) {
        sessionMetrics.add(metric)
        _currentMetrics.value = metric
    }

    /**
     * Manually triggers a single sample capture on-demand (used by tests or checkpoints).
     */
    fun sampleNow(): PerformanceMetric {
        val position = playbackPositionSupplier?.invoke()
        val metric = PerformanceMetric(
            timestamp = System.currentTimeMillis(),
            cpuUsagePercent = cpuCollector.sampleCpuUsage(),
            gpuUsagePercent = gpuCollector.sampleGpuUsage(),
            deviceTemperatureCelsius = temperatureCollector.sampleTemperature(),
            videoId = activeVideoId,
            playbackPositionMs = position
        )
        recordSnapshot(metric)
        return metric
    }

    private fun startSamplingLoop() {
        samplingJob?.cancel()
        samplingJob = scope.launch {
            while (isActive && isMonitoring) {
                delay(samplingIntervalMs)

                if (!isMonitoring) break
                if (isPaused) continue

                val position = playbackPositionSupplier?.invoke()
                val cpu = cpuCollector.sampleCpuUsage()
                val gpu = gpuCollector.sampleGpuUsage()
                val temp = temperatureCollector.sampleTemperature()

                val metric = PerformanceMetric(
                    timestamp = System.currentTimeMillis(),
                    cpuUsagePercent = cpu,
                    gpuUsagePercent = gpu,
                    deviceTemperatureCelsius = temp,
                    videoId = activeVideoId,
                    playbackPositionMs = position
                )

                sessionMetrics.add(metric)
                _currentMetrics.value = metric

                com.haptix.app.util.HaptiXLog.d(
                    "PERFORMANCE_SAMPLE: video=$activeVideoId pos=${position}ms " +
                            "cpu=${cpu?.let { "%.1f%%".format(it) } ?: "null"} " +
                            "gpu=${gpu?.let { "%.1f%%".format(it) } ?: "null"} " +
                            "temp=${temp?.let { "%.1f°C".format(it) } ?: "null"}"
                )
            }
        }
    }
}
