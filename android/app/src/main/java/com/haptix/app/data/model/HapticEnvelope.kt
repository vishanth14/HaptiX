package com.haptix.app.data.model

/**
 * Encapsulates a parametric amplitude envelope defining the continuous shape
 * (Attack -> Sustain -> Release) of a tactile stimulus.
 *
 * Guarantees physical plausibility:
 * - attackMs >= 0
 * - sustainMs >= 0
 * - releaseMs >= 0
 * - attackMs + sustainMs + releaseMs <= durationMs
 *
 * @param attackMs Milliseconds taken to smoothly ramp from 0 to peak intensity.
 * @param sustainMs Milliseconds spent sustaining the peak intensity (or body).
 * @param releaseMs Milliseconds taken to smoothly ramp down from peak intensity to 0.
 */
data class HapticEnvelope(
    val attackMs: Long = 0L,
    val sustainMs: Long = 0L,
    val releaseMs: Long = 0L,
    val curveIn: String = "EASE_IN_OUT",
    val curveOut: String = "EASE_IN_OUT"
) {
    /**
     * Total span of the envelope phases in milliseconds.
     */
    val totalMs: Long get() = attackMs + sustainMs + releaseMs

    init {
        require(attackMs >= 0L) { "attackMs must be non-negative, was $attackMs" }
        require(sustainMs >= 0L) { "sustainMs must be non-negative, was $sustainMs" }
        require(releaseMs >= 0L) { "releaseMs must be non-negative, was $releaseMs" }
    }

    /**
     * Validates that the envelope parameters are physically bounded by the total event duration.
     *
     * @param durationMs The parent event duration in milliseconds.
     * @throws IllegalArgumentException if the envelope phases exceed the duration.
     */
    fun validateForDuration(durationMs: Long) {
        require(totalMs <= durationMs) {
            "Envelope attackMs ($attackMs) + sustainMs ($sustainMs) + releaseMs ($releaseMs) = ${totalMs}ms cannot exceed event durationMs (${durationMs}ms)"
        }
    }

    /**
     * Converts to a map representation for parameters storage or JSON serialization.
     */
    fun toMap(): Map<String, Long> = mapOf(
        "attackMs" to attackMs,
        "sustainMs" to sustainMs,
        "releaseMs" to releaseMs
    )

    override fun toString(): String = "$attackMs/$sustainMs/$releaseMs"
}
