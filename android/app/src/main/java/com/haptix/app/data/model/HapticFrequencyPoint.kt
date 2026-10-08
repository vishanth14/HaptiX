package com.haptix.app.data.model

/**
 * Represents a single frequency-controlled vibrotactile stimulus point.
 *
 * In accordance with Android 16 (API 36) [android.os.VibrationEffect.WaveformEnvelopeBuilder],
 * each point encapsulates an experimental driving frequency (Hz), normalized amplitude (0.0f to 1.0f),
 * onset timestamp (ms), and duration (ms).
 *
 * Scientific Distinction:
 * - Stimulus frequencies (e.g. 150-250 Hz) are experimentally selected by HaptiX based on
 *   human vibrotactile perception literature (Pacinian mechanoreceptor sensitivity band).
 * - Physical actuator resonant frequency (f0) is strictly device-dependent and must be queried
 *   at runtime from Android hardware APIs where available, not assumed as a fixed constant.
 *
 * @param startTimeMs Event onset timestamp in milliseconds relative to timeline start.
 * @param durationMs Duration of the frequency pulse/transition in milliseconds.
 * @param frequencyHz Experimental driving frequency in Hertz (must be > 0 and finite).
 * @param amplitude Normalized vibration amplitude / acceleration (0.0f off to 1.0f maximum).
 * @param parameters Extensible key-value parameter map for research provenance and metadata.
 */
data class HapticFrequencyPoint(
    val startTimeMs: Long,
    val durationMs: Long,
    val frequencyHz: Float,
    val amplitude: Float = 1.0f,
    val parameters: Map<String, Any> = emptyMap()
) {
    /**
     * Backward-compatible timestamp alias in milliseconds.
     */
    val timestampMs: Long get() = startTimeMs

    /**
     * Computed end time of the frequency stimulus point on the timeline.
     */
    val endTimeMs: Long get() = startTimeMs + durationMs

    /**
     * Explicit scientific provenance descriptor (e.g. "perception-literature motivated",
     * "experimentally selected by HaptiX", or "device-specific").
     */
    val provenance: String? get() = parameters["provenance"] as? String

    init {
        require(startTimeMs >= 0L) { "startTimeMs must be non-negative, was $startTimeMs" }
        require(durationMs >= 0L) { "durationMs must be non-negative, was $durationMs" }
        require(!frequencyHz.isNaN() && !frequencyHz.isInfinite() && frequencyHz > 0f) {
            "frequencyHz must be positive and finite, was $frequencyHz"
        }
        require(!amplitude.isNaN() && amplitude in 0.0f..1.0f) {
            "amplitude must be between 0.0 and 1.0, was $amplitude"
        }
    }

    /**
     * Converts this frequency point into a platform-neutral [HapticEvent].
     * Maps normalized frequency (where 200 Hz is nominal midpoint) into sharpness parameter,
     * while storing explicit frequencyHz and provenance in parameters.
     */
    fun toHapticEvent(): HapticEvent {
        // Map typical LRA range (100Hz - 300Hz) to normalized sharpness (0.0f - 1.0f)
        val normalizedSharpness = ((frequencyHz - 100f) / 200f).coerceIn(0.0f, 1.0f)
        val eventParams = parameters.toMutableMap().apply {
            put("frequencyHz", frequencyHz)
            put("amplitude", amplitude)
            provenance?.let { put("provenance", it) }
        }
        val pAttack = (parameters["attackMs"] as? Number)?.toLong() ?: 0L
        val pSustain = (parameters["sustainMs"] as? Number)?.toLong() ?: 0L
        val pRelease = (parameters["releaseMs"] as? Number)?.toLong() ?: 0L
        val sType = SemanticHapticType.fromString(parameters["hapticType"] as? String ?: parameters["semanticType"] as? String)

        return HapticEvent(
            type = if (durationMs > 80L) HapticEventType.CONTINUOUS else HapticEventType.TRANSIENT,
            startTimeMs = startTimeMs,
            durationMs = durationMs,
            intensity = amplitude,
            sharpness = normalizedSharpness,
            semanticType = sType,
            attackMs = pAttack,
            sustainMs = pSustain,
            releaseMs = pRelease,
            frequencyHz = frequencyHz,
            parameters = eventParams
        )
    }
}
