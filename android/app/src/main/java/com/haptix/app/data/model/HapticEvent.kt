package com.haptix.app.data.model

/**
 * Platform-independent enumeration of haptic feedback event types.
 *
 * Designed to capture both discrete impulses and continuous vibratory textures
 * without binding to platform-specific definitions (e.g. CoreHaptics AHAP).
 */
enum class HapticEventType {
    TRANSIENT,
    CONTINUOUS
}

/**
 * Platform-independent representation of a single timed haptic event aligned with playback.
 *
 * This model captures core physical actuation properties (timing, intensity, sharpness)
 * in a standardized, platform-neutral manner, ready to be populated by the finalized
 * Android haptic JSON format.
 *
 * @param type Event type category (transient pulse or continuous vibration).
 * @param startTimeMs Event onset timestamp in milliseconds relative to timeline start.
 * @param durationMs Duration of the event in milliseconds (0 for instantaneous transient events).
 * @param intensity Normalized vibration amplitude/strength (0.0f minimum to 1.0f maximum).
 * @param sharpness Normalized frequency sensation / sharpness (0.0f dull/low-pitch to 1.0f crisp/high-pitch).
 * @param parameters Optional extensible control parameters (e.g., curves, frequency modulation, attack/decay).
 */
data class HapticEvent(
    val type: HapticEventType = HapticEventType.TRANSIENT,
    val startTimeMs: Long,
    val durationMs: Long = 0L,
    val intensity: Float = 1.0f,
    val sharpness: Float = 0.5f,
    val parameters: Map<String, Any> = emptyMap()
) {
    /**
     * Backward-compatible alias for timeline timestamp in milliseconds.
     */
    val timestampMs: Long get() = startTimeMs

    /**
     * Computed end time of the haptic event on the timeline.
     */
    val endTimeMs: Long get() = startTimeMs + durationMs

    init {
        require(startTimeMs >= 0L) { "startTimeMs must be non-negative, was $startTimeMs" }
        require(durationMs >= 0L) { "durationMs must be non-negative, was $durationMs" }
        require(intensity in 0.0f..1.0f) { "intensity must be between 0.0 and 1.0, was $intensity" }
        require(sharpness in 0.0f..1.0f) { "sharpness must be between 0.0 and 1.0, was $sharpness" }
    }
}
