package com.haptix.app.data.model

import com.haptix.app.domain.model.HapticSource

/**
 * Represents a complete sequence of platform-independent haptic events for a specific video stimulus.
 *
 * Consumed identically by:
 * - Android Converter -> AndroidHapticPlayer (WaveformEnvelopeBuilder / calibrated resonant fallback)
 * - iOS Converter -> CoreHaptics AHAP (.ahap JSON structure)
 *
 * @param videoId Associated video stimulus identifier.
 * @param version Format version descriptor.
 * @param source Origin of the pattern ([HapticSource.CURATED], [HapticSource.GENERATED], or [HapticSource.LEGACY_MANUAL]).
 * @param events Chronological collection of [HapticEvent] objects comprising the pattern.
 */
data class HapticPattern(
    val videoId: String,
    val version: String = "1.0",
    val source: HapticSource = HapticSource.LEGACY_MANUAL,
    val events: List<HapticEvent> = emptyList(),
    val videoDurationMs: Long? = null
) {
    /**
     * Total number of haptic events within the pattern.
     */
    val eventCount: Int get() = events.size

    /**
     * Total pattern span from 0 to the end of the last haptic event in milliseconds.
     */
    val totalDurationMs: Long
        get() = videoDurationMs ?: (events.maxOfOrNull { it.endTimeMs } ?: 0L)

    /**
     * Returns events chronologically ordered by start time.
     */
    fun sortedEvents(): List<HapticEvent> = events.sortedBy { it.startTimeMs }
}
