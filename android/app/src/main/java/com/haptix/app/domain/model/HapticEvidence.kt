package com.haptix.app.domain.model

/**
 * Encapsulates the multimodal research provenance and evidence behind a synthesized haptic event.
 *
 * Guarantees full scientific traceability:
 * - Links to specific scene IDs and keyframe timestamps.
 * - Documents visual, auditory, and optical flow descriptors.
 * - Captures dynamic modal reliability and cross-modal semantic similarity scores.
 */
data class HapticEvidence(
    val sceneId: Int? = null,
    val visualEvidence: String? = null,
    val audioEvidence: String? = null,
    val motionEvidence: String? = null,
    val frameTimestampsMs: List<Long> = emptyList(),
    val audioWindowMs: Pair<Long, Long>? = null,
    val visualReliability: Float = 0.5f,
    val audioReliability: Float = 0.5f,
    val crossModalSimilarity: Float = 0.5f,
    val confidence: Float = 1.0f
) {
    init {
        require(visualReliability in 0.0f..1.0f) { "visualReliability must be in 0.0..1.0, was $visualReliability" }
        require(audioReliability in 0.0f..1.0f) { "audioReliability must be in 0.0..1.0, was $audioReliability" }
        require(crossModalSimilarity in 0.0f..1.0f) { "crossModalSimilarity must be in 0.0..1.0, was $crossModalSimilarity" }
        require(confidence in 0.0f..1.0f) { "confidence must be in 0.0..1.0, was $confidence" }
    }

    /**
     * Converts to an untyped parameter map for serialization or logging.
     */
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        sceneId?.let { map["sceneId"] = it }
        visualEvidence?.let { map["visualEvidence"] = it }
        audioEvidence?.let { map["audioEvidence"] = it }
        motionEvidence?.let { map["motionEvidence"] = it }
        if (frameTimestampsMs.isNotEmpty()) {
            map["frameTimestampsMs"] = frameTimestampsMs
        }
        audioWindowMs?.let {
            map["audioWindowStartMs"] = it.first
            map["audioWindowEndMs"] = it.second
        }
        map["visualReliability"] = visualReliability
        map["audioReliability"] = audioReliability
        map["crossModalSimilarity"] = crossModalSimilarity
        map["confidence"] = confidence
        return map
    }

    companion object {
        /**
         * Reconstructs [HapticEvidence] from an extensible parameters map.
         */
        private fun extractStringOrArray(item: Any?): String? {
            return when (item) {
                is String -> item
                is List<*> -> item.filterNotNull().joinToString("; ")
                is org.json.JSONArray -> (0 until item.length()).map { item.optString(it) }.joinToString("; ")
                else -> null
            }
        }

        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): HapticEvidence {
            val sceneId = (map["sceneId"] as? Number)?.toInt()
            val visualEvidence = extractStringOrArray(map["visualEvidence"]) ?: extractStringOrArray(map["visual"])
            val audioEvidence = extractStringOrArray(map["audioEvidence"]) ?: extractStringOrArray(map["audio"])
            val motionEvidence = extractStringOrArray(map["motionEvidence"]) ?: extractStringOrArray(map["motion"])
            val frameTimestampsMs = when (val frames = map["frameTimestampsMs"]) {
                is List<*> -> frames.mapNotNull { (it as? Number)?.toLong() }
                is org.json.JSONArray -> (0 until frames.length()).map { frames.optLong(it) }
                else -> emptyList()
            }
            val audioStart = (map["audioWindowStartMs"] as? Number)?.toLong()
            val audioEnd = (map["audioWindowEndMs"] as? Number)?.toLong()
            val audioWindow = if (audioStart != null && audioEnd != null) Pair(audioStart, audioEnd) else null
            val visualReliability = (map["visualReliability"] as? Number)?.toFloat() ?: 0.5f
            val audioReliability = (map["audioReliability"] as? Number)?.toFloat() ?: 0.5f
            val crossModalSimilarity = (map["crossModalSimilarity"] as? Number)?.toFloat() ?: 0.5f
            val confidence = (map["confidence"] as? Number)?.toFloat()
                ?: (map["fusionConfidence"] as? Number)?.toFloat()
                ?: 1.0f

            return HapticEvidence(
                sceneId = sceneId,
                visualEvidence = visualEvidence,
                audioEvidence = audioEvidence,
                motionEvidence = motionEvidence,
                frameTimestampsMs = frameTimestampsMs,
                audioWindowMs = audioWindow,
                visualReliability = visualReliability.coerceIn(0.0f, 1.0f),
                audioReliability = audioReliability.coerceIn(0.0f, 1.0f),
                crossModalSimilarity = crossModalSimilarity.coerceIn(0.0f, 1.0f),
                confidence = confidence.coerceIn(0.0f, 1.0f)
            )
        }
    }
}
