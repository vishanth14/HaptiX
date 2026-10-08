package com.haptix.app.domain.model

/**
 * Categorization of the origin and methodology used to produce a haptic pattern.
 *
 * All sources converge into the identical platform-independent [HapticPattern].
 */
enum class HapticSource {
    /**
     * Pre-validated, curated reference stimulus (e.g. F1 onboard telemetry, gold-standard benchmarks).
     */
    CURATED,

    /**
     * Synthesized via the HaptiX multimodal processing pipeline (video + audio analysis & fusion).
     */
    GENERATED,

    /**
     * Legacy manually-authored reference data (e.g. historical Koji demonstration keypoints).
     * Explicitly preserved to prevent regression and enable scientific comparative studies.
     */
    LEGACY_MANUAL;

    companion object {
        fun fromString(value: String?): HapticSource {
            if (value.isNullOrBlank()) return LEGACY_MANUAL
            val upper = value.trim().uppercase()
            return when {
                upper.contains("CURATED") -> CURATED
                upper.contains("GEN") || upper.contains("PIPELINE") -> GENERATED
                else -> LEGACY_MANUAL
            }
        }
    }
}
