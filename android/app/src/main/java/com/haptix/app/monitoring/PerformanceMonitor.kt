package com.haptix.app.monitoring

import com.haptix.app.data.model.PerformanceMetric
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors and captures device performance metrics (frame stability, memory, CPU)
 * during experimental video and haptic playback sessions.
 */
class PerformanceMonitor {
    private val _currentMetrics = MutableStateFlow<PerformanceMetric?>(null)
    val currentMetrics: Flow<PerformanceMetric?> = _currentMetrics.asStateFlow()

    private val sessionMetrics = mutableListOf<PerformanceMetric>()

    /**
     * Initiates metric recording for the given video playback session.
     */
    fun startMonitoring(videoId: String) {
        sessionMetrics.clear()
    }

    /**
     * Stops active metric recording and returns aggregated session metrics.
     */
    fun stopMonitoring(): List<PerformanceMetric> {
        return sessionMetrics.toList()
    }

    /**
     * Records a single performance snapshot.
     */
    fun recordSnapshot(metric: PerformanceMetric) {
        sessionMetrics.add(metric)
        _currentMetrics.value = metric
    }
}
