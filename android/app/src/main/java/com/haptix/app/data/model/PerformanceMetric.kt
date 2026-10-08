package com.haptix.app.data.model

/**
 * Captures device performance metrics recorded during active experimental playback.
 *
 * All hardware telemetry values are strictly nullable:
 * if a metric is not available on the device or cannot be sampled reliably without root privileges,
 * it is represented as null rather than a fabricated or zero default.
 *
 * @param timestamp Epoch timestamp in milliseconds when sample was acquired.
 * @param cpuUsagePercent System or process CPU utilization percentage (0.0f - 100.0f), or null if unavailable.
 * @param gpuUsagePercent GPU utilization percentage if supported/available on device, null otherwise.
 * @param deviceTemperatureCelsius Device thermal sensor temperature in degrees Celsius, or null if unavailable.
 * @param videoId Associated experimental video identifier.
 * @param playbackPositionMs Playback timeline position in milliseconds at the moment of sample collection.
 * @param fps Measured render frames per second, or null if unavailable.
 * @param memoryUsageMb Allocated application memory in megabytes, or null if unavailable.
 */
data class PerformanceMetric(
    val timestamp: Long = System.currentTimeMillis(),
    val cpuUsagePercent: Float? = null,
    val gpuUsagePercent: Float? = null,
    val deviceTemperatureCelsius: Float? = null,
    val videoId: String = "",
    val playbackPositionMs: Long? = null,
    val fps: Float? = null,
    val memoryUsageMb: Long? = null
) {
    /**
     * Backward-compatible alias for timestamp in milliseconds.
     */
    val timestampMs: Long get() = timestamp
}
