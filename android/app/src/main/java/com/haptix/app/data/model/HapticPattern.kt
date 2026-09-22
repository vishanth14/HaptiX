package com.haptix.app.data.model

/**
 * Represents a complete sequence of haptic events for a specific video stimuli.
 *
 * This pattern acts as the domain representation that will be instantiated once
 * the Android-specific haptic JSON format is parsed.
 *
 * @param videoId Associated video stimulus identifier.
 * @param version Format version descriptor.
 * @param events Chronological collection of [HapticEvent] objects comprising the pattern.
 */
data class HapticPattern(
    val videoId: String,
    val version: String = "1.0",
    val events: List<HapticEvent> = emptyList()
) {
    /**
     * Total number of haptic events within the pattern.
     */
    val eventCount: Int get() = events.size

    /**
     * Total pattern span from 0 to the end of the last haptic event in milliseconds.
     */
    val totalDurationMs: Long
        get() = events.maxOfOrNull { it.endTimeMs } ?: 0L

    /**
     * Returns events chronologically ordered by start time.
     */
    fun sortedEvents(): List<HapticEvent> = events.sortedBy { it.startTimeMs }
}
