package com.haptix.app.data.model

import com.haptix.app.domain.model.HapticEvidence
import java.util.UUID

/**
 * Platform-independent enumeration of haptic feedback event types.
 */
enum class HapticEventType {
    TRANSIENT,
    CONTINUOUS
}

/**
 * Platform-independent canonical representation of a single timed haptic event.
 *
 * Guarantees physical plausibility and research traceability:
 * - Free of Android OS or iOS runtime dependencies.
 * - Enforces smooth Attack-Sustain-Release envelopes (eliminating abrupt 0 -> MAX -> 0 spikes).
 * - Retains multimodal evidence tracking (visual, audio, optical flow, reliability).
 *
 * @param id Unique event identifier (UUID by default).
 * @param startTimeMs Event onset timestamp in milliseconds relative to timeline start.
 * @param durationMs Duration of the event in milliseconds (0 for instantaneous transient impulses).
 * @param semanticType Semantic intent category (e.g. SMOOTH, JUMP, LAUNCH, HEAVY_IMPACT).
 * @param intensity Normalized vibration amplitude/strength (0.0f minimum to 1.0f maximum).
 * @param attackMs Milliseconds taken to smoothly ramp from 0 to peak intensity.
 * @param sustainMs Milliseconds spent sustaining the peak intensity body.
 * @param releaseMs Milliseconds taken to smoothly ramp down from peak intensity to 0.
 * @param frequencyHz Optional target vibration frequency in Hz (e.g. 150-240 Hz research stimuli).
 * @param sharpness Normalized frequency sensation / sharpness (0.0f dull/low-pitch to 1.0f crisp/high-pitch).
 * @param confidence Multimodal synthesis confidence score (0.0f to 1.0f).
 * @param evidence Traceability metadata explaining why this event was generated.
 * @param sourceModalities List of sensory modalities confirming this event (e.g. "visual", "audio").
 * @param type Legacy category (TRANSIENT or CONTINUOUS) for backwards compatibility.
 * @param parameters Extensible parameter map for platform-specific adapters.
 */
data class HapticEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: HapticEventType = HapticEventType.TRANSIENT,
    val startTimeMs: Long,
    val durationMs: Long = 0L,
    val intensity: Float = 1.0f,
    val sharpness: Float = 0.5f,
    val semanticType: SemanticHapticType = SemanticHapticType.SMOOTH,
    val attackMs: Long = 0L,
    val sustainMs: Long = 0L,
    val releaseMs: Long = 0L,
    val frequencyHz: Float? = null,
    val confidence: Float = 1.0f,
    val evidence: HapticEvidence? = null,
    val sourceModalities: List<String> = emptyList(),
    val parameters: Map<String, Any> = emptyMap(),
    val peakTimeMs: Long? = null
) {
    /**
     * Backward-compatible alias for timeline timestamp in milliseconds.
     */
    val timestampMs: Long get() = startTimeMs

    /**
     * Resolved peak timestamp for transient tactile impulses and playline markers.
     */
    val effectivePeakTimeMs: Long
        get() = peakTimeMs
            ?: (parameters["peakTimeMs"] as? Number)?.toLong()
            ?: (parameters["peakTime"] as? Number)?.toLong()
            ?: startTimeMs

    /**
     * Computed end time of the haptic event on the timeline.
     */
    val endTimeMs: Long get() = startTimeMs + durationMs

    /**
     * Synthesized or explicit [HapticEnvelope] for this event.
     */
    val envelope: HapticEnvelope
        get() = if (attackMs > 0L || sustainMs > 0L || releaseMs > 0L) {
            val envMap = parameters["envelope"] as? Map<*, *>
            val cIn = envMap?.get("curveIn") as? String ?: "EASE_IN_OUT"
            val cOut = envMap?.get("curveOut") as? String ?: "EASE_IN_OUT"
            HapticEnvelope(attackMs, sustainMs, releaseMs, cIn, cOut)
        } else {
            val envMap = parameters["envelope"] as? Map<*, *>
            val pAttack = (envMap?.get("attackMs") as? Number)?.toLong()
                ?: (parameters["attackMs"] as? Number)?.toLong() ?: 0L
            val pSustain = (envMap?.get("sustainMs") as? Number)?.toLong()
                ?: (parameters["sustainMs"] as? Number)?.toLong() ?: 0L
            val pRelease = (envMap?.get("releaseMs") as? Number)?.toLong()
                ?: (parameters["releaseMs"] as? Number)?.toLong() ?: 0L
            val cIn = (envMap?.get("curveIn") as? String) ?: "EASE_IN_OUT"
            val cOut = (envMap?.get("curveOut") as? String) ?: "EASE_IN_OUT"
            HapticEnvelope(pAttack, pSustain, pRelease, cIn, cOut)
        }

    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(startTimeMs >= 0L) { "startTimeMs must be non-negative, was $startTimeMs" }
        require(durationMs >= 0L) { "durationMs must be non-negative, was $durationMs" }
        require(intensity in 0.0f..1.0f) { "intensity must be between 0.0 and 1.0, was $intensity" }
        require(sharpness in 0.0f..1.0f) { "sharpness must be between 0.0 and 1.0, was $sharpness" }
        require(attackMs >= 0L) { "attackMs must be non-negative, was $attackMs" }
        require(sustainMs >= 0L) { "sustainMs must be non-negative, was $sustainMs" }
        require(releaseMs >= 0L) { "releaseMs must be non-negative, was $releaseMs" }
        if (durationMs > 0L && (attackMs > 0L || sustainMs > 0L || releaseMs > 0L)) {
            val totalEnv = attackMs + sustainMs + releaseMs
            require(totalEnv <= durationMs) {
                "Envelope attack ($attackMs) + sustain ($sustainMs) + release ($releaseMs) = ${totalEnv}ms exceeds durationMs (${durationMs}ms)"
            }
        }
        if (frequencyHz != null) {
            require(frequencyHz > 0.0f && !frequencyHz.isNaN() && !frequencyHz.isInfinite()) {
                "frequencyHz must be positive and finite, was $frequencyHz"
            }
        }
        require(confidence in 0.0f..1.0f) { "confidence must be between 0.0 and 1.0, was $confidence" }
    }
}
