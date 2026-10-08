package com.haptix.app.domain.model

/**
 * Platform-independent representation of a segmented video scene interval.
 */
data class Scene(
    val sceneId: Int,
    val startFrame: Int,
    val endFrame: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val cutConfidence: Float = 1.0f
) {
    init {
        require(sceneId >= 0) { "sceneId must be non-negative" }
        require(startFrame >= 0) { "startFrame must be non-negative" }
        require(endFrame >= startFrame) { "endFrame ($endFrame) must be >= startFrame ($startFrame)" }
        require(startTimeMs >= 0L) { "startTimeMs must be non-negative" }
        require(endTimeMs >= startTimeMs) { "endTimeMs ($endTimeMs) must be >= startTimeMs ($startTimeMs)" }
        require(cutConfidence in 0.0f..1.0f) { "cutConfidence must be between 0.0 and 1.0" }
    }

    val durationMs: Long get() = endTimeMs - startTimeMs
    val frameCount: Int get() = endFrame - startFrame + 1
}

/**
 * Observation data captured from an extracted keyframe.
 */
data class FrameObservation(
    val frameIndex: Int,
    val timestampMs: Long,
    val sceneId: Int,
    val width: Int = 0,
    val height: Int = 0,
    val averageLuminance: Float = 0.5f,
    val dominantColors: List<String> = emptyList()
) {
    init {
        require(frameIndex >= 0) { "frameIndex must be non-negative" }
        require(timestampMs >= 0L) { "timestampMs must be non-negative" }
        require(sceneId >= 0) { "sceneId must be non-negative" }
        require(averageLuminance in 0.0f..1.0f) { "averageLuminance must be in 0.0..1.0" }
    }
}

/**
 * Optical flow motion observation derived from video frame analysis.
 */
data class MotionObservation(
    val timestampMs: Long,
    val magnitude: Float,
    val peakMagnitude: Float = magnitude,
    val directionDeg: Float = 0.0f,
    val dx: Float = 0.0f,
    val dy: Float = 0.0f,
    val verticalBias: Float = 0.0f,    // Positive: upward (jump/launch), Negative: downward (landing/drop)
    val horizontalBias: Float = 0.0f,  // Positive: rightward, Negative: leftward
    val activeRegionRatio: Float = 0.0f,
    val isRealRaft: Boolean = false,
    val modelName: String = "Farneback (Fallback)"
) {
    init {
        require(timestampMs >= 0L) { "timestampMs must be non-negative" }
        require(magnitude >= 0.0f) { "magnitude must be non-negative" }
        require(peakMagnitude >= 0.0f) { "peakMagnitude must be non-negative" }
        require(activeRegionRatio in 0.0f..1.0f) { "activeRegionRatio must be in 0.0..1.0" }
        require(verticalBias in -1.0f..1.0f) { "verticalBias must be in -1.0..1.0" }
        require(horizontalBias in -1.0f..1.0f) { "horizontalBias must be in -1.0..1.0" }
    }
}

/**
 * High-level visual semantic observation (e.g. from LLaVA or semantic rule engine).
 */
data class VisualSemanticObservation(
    val timestampMs: Long,
    val sceneId: Int,
    val description: String,
    val characters: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val motionCues: List<String> = emptyList(),
    val environment: List<String> = emptyList(),
    val salience: Float = 0.5f,
    val isRealLlava: Boolean = false,
    val modelName: String = "VisualSemanticFallback"
) {
    init {
        require(timestampMs >= 0L) { "timestampMs must be non-negative" }
        require(sceneId >= 0) { "sceneId must be non-negative" }
        require(salience in 0.0f..1.0f) { "salience must be in 0.0..1.0" }
    }
}

/**
 * Low-level physical audio descriptors extracted from MP4 audio stream.
 */
data class AudioObservation(
    val timestampMs: Long,
    val durationMs: Long,
    val energyMse: Float,
    val spectralCentroidHz: Float,
    val zeroCrossingRate: Float,
    val spectralFlux: Float,
    val dialogueRatio: Float = 0.0f,
    val sfxBgmRatio: Float = 1.0f - dialogueRatio
) {
    init {
        require(timestampMs >= 0L) { "timestampMs must be non-negative" }
        require(durationMs >= 0L) { "durationMs must be non-negative" }
        require(energyMse >= 0.0f) { "energyMse must be non-negative" }
        require(spectralCentroidHz >= 0.0f) { "spectralCentroidHz must be non-negative" }
        require(zeroCrossingRate >= 0.0f) { "zeroCrossingRate must be non-negative" }
        require(spectralFlux >= 0.0f) { "spectralFlux must be non-negative" }
        require(dialogueRatio in 0.0f..1.0f) { "dialogueRatio must be in 0.0..1.0" }
    }
}

/**
 * Audio semantic observation (e.g. from Whisper Tiny speech recognition or sound captioning).
 */
data class AudioSemanticObservation(
    val timestampMs: Long,
    val caption: String,
    val speechDetected: Boolean = false,
    val soundClasses: List<String> = emptyList(),
    val confidence: Float = 1.0f,
    val isRealWhisper: Boolean = false
) {
    init {
        require(timestampMs >= 0L) { "timestampMs must be non-negative" }
        require(confidence in 0.0f..1.0f) { "confidence must be in 0.0..1.0" }
    }
}

/**
 * Cross-modal fusion observation linking visual and auditory evidence.
 */
data class FusionObservation(
    val timestampMs: Long,
    val visualReliability: Float,
    val audioReliability: Float,
    val crossModalSimilarity: Float,
    val temporalContinuityScore: Float = 1.0f,
    val fusedConfidence: Float = 0.8f,
    val dominantModality: String = "MULTIMODAL"
) {
    init {
        require(timestampMs >= 0L) { "timestampMs must be non-negative" }
        require(visualReliability in 0.0f..1.0f) { "visualReliability must be in 0.0..1.0" }
        require(audioReliability in 0.0f..1.0f) { "audioReliability must be in 0.0..1.0" }
        require(crossModalSimilarity in 0.0f..1.0f) { "crossModalSimilarity must be in 0.0..1.0" }
        require(temporalContinuityScore in 0.0f..1.0f) { "temporalContinuityScore must be in 0.0..1.0" }
        require(fusedConfidence in 0.0f..1.0f) { "fusedConfidence must be in 0.0..1.0" }
    }
}
