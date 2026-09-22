package com.haptix.app.data.model

/**
 * Domain representation of a research-backed sequence of frequency-calibrated haptic stimuli.
 *
 * Encapsulates full research provenance (source, literature citation URL, descriptions)
 * alongside an ordered collection of [HapticFrequencyPoint] instances.
 *
 * @param videoId Associated video stimulus identifier.
 * @param source Name or citation of the researched frequency dataset.
 * @param sourceUrl URL to public documentation or peer-reviewed literature.
 * @param description Detailed context describing the tactile stimuli design.
 * @param frequencyUnit Frequency physical unit (typically "Hz").
 * @param timingUnit Time unit (typically "ms").
 * @param version Schema/format version descriptor.
 * @param points Chronological list of frequency stimulus points.
 */
data class HapticFrequencyPattern(
    val videoId: String,
    val source: String = "AOSP Haptics & Pacinian Vibrotactile Research",
    val sourceUrl: String = "https://source.android.com/devices/sensors/haptics",
    val description: String = "",
    val frequencyUnit: String = "Hz",
    val timingUnit: String = "ms",
    val version: String = "1.0",
    val points: List<HapticFrequencyPoint> = emptyList()
) {
    /**
     * Total number of frequency points in this pattern.
     */
    val pointCount: Int get() = points.size

    /**
     * Total pattern span from 0 to the end of the last frequency event in milliseconds.
     */
    val totalDurationMs: Long
        get() = points.maxOfOrNull { it.endTimeMs } ?: 0L

    /**
     * Returns points chronologically ordered by start timestamp.
     */
    fun sortedPoints(): List<HapticFrequencyPoint> = points.sortedBy { it.startTimeMs }

    /**
     * Converts this frequency pattern into the standard HaptiX [HapticPattern].
     */
    fun toHapticPattern(): HapticPattern {
        return HapticPattern(
            videoId = videoId,
            version = version,
            events = points.map { it.toHapticEvent() }
        )
    }
}
