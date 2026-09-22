package com.haptix.app.data.model

/**
 * Captures device performance metrics recorded during experimental playback.
 *
 * @param timestamp Epoch timestamp in milliseconds when sample was acquired.
 * @param cpuUsagePercent System CPU utilization percentage (0.0f - 100.0f).
 * @param gpuUsagePercent GPU utilization percentage if supported/available on device, null otherwise.
 * @param deviceTemperatureCelsius Device thermal sensor temperature in degrees Celsius.
 * @param videoId Associated experimental video identifier.
 * @param fps Measured render frames per second.
 * @param memoryUsageMb Allocated application memory in megabytes.
 */
data class PerformanceMetric(
    val timestamp: Long = System.currentTimeMillis(),
    val cpuUsagePercent: Float = 0f,
    val gpuUsagePercent: Float? = null,
    val deviceTemperatureCelsius: Float = 0f,
    val videoId: String = "",
    val fps: Float = 0f,
    val memoryUsageMb: Long = 0L
) {
    /**
     * Backward-compatible alias for timestamp in milliseconds.
     */
    val timestampMs: Long get() = timestamp
}
