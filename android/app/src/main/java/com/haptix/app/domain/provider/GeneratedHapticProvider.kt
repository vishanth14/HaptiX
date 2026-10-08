package com.haptix.app.domain.provider

import android.content.Context
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.parser.FrequencyPatternParser
import com.haptix.app.domain.model.HapticSource
import java.io.File

/**
 * Provider supplying pipeline-generated haptic patterns derived from multimodal analysis
 * (e.g. Koji animation synthesis, automated YouTube video ingestion jobs).
 *
 * Supports:
 * - Bundled assets (`haptics/<videoId>_haptic_representation.json`)
 * - Dynamic runtime pattern registration
 * - Local filesystem caching in app files/cache directories
 */
class GeneratedHapticProvider(
    private val context: Context? = null,
    private val assetReader: ((String) -> String?)? = null,
    private val dynamicPatterns: MutableMap<String, HapticPattern> = mutableMapOf()
) : HapticSourceProvider {

    override val source: HapticSource = HapticSource.GENERATED

    /**
     * Registers a dynamically generated pattern into memory.
     */
    fun registerPattern(pattern: HapticPattern) {
        dynamicPatterns[pattern.videoId] = pattern.copy(source = HapticSource.GENERATED)
    }

    override suspend fun getPattern(videoId: String): HapticPattern? {
        return getPattern(videoId, null)
    }

    override suspend fun getPattern(videoId: String, hapticConfigResName: String?): HapticPattern? {
        // 1. Dynamic in-memory registration check
        val dynamic = dynamicPatterns[videoId]
        if (dynamic != null) return dynamic

        // Clean videoId
        val cleanId = videoId.removePrefix("yt_").trim()

        // 2. Local filesystem storage check (for completed background processing jobs)
        if (context != null) {
            val fileCandidates = listOf(
                File(context.filesDir, "jobs/$videoId/haptics/haptic_representation.json"),
                File(context.filesDir, "jobs/yt_$cleanId/haptics/haptic_representation.json"),
                File(context.cacheDir, "haptics/${videoId}_haptic_representation.json"),
                File(context.cacheDir, "haptics/yt_${cleanId}_haptic_representation.json")
            )
            for (f in fileCandidates) {
                if (f.exists() && f.canRead()) {
                    val result = FrequencyPatternParser.parseToHapticPattern(f.readText())
                    if (result.isSuccess) {
                        return result.getOrThrow().copy(source = HapticSource.GENERATED)
                    }
                }
            }
        }

        // 3. Asset candidates check with multi-id normalization
        val searchIds = when {
            videoId.contains("f1", ignoreCase = true) -> listOf(videoId, "f1_2025_haptic_trailer", "f1_2025", "f1", cleanId)
            videoId.contains("koji", ignoreCase = true) -> listOf(videoId, "koji", "video_koji", "koji_demo", cleanId)
            else -> listOf(videoId, cleanId)
        }.distinct()

        val candidates = mutableListOf<String>()
        if (!hapticConfigResName.isNullOrBlank()) {
            val cleanConfig = hapticConfigResName.removeSuffix(".json").trim()
            candidates.add("haptics/$cleanConfig.json")
            candidates.add("haptics/$cleanConfig")
        }
        for (sid in searchIds) {
            if (sid.contains("koji", ignoreCase = true)) {
                candidates.add("haptics/koji_haptic_timeline.json")
            } else if (sid.contains("f1", ignoreCase = true)) {
                candidates.add("haptics/f1_haptic_timeline.json")
            }
            candidates.add("haptics/${sid}_haptic_timeline.json")
            candidates.add("haptics/yt_${sid}_haptic_representation.json")
            candidates.add("haptics/${sid}.json")
        }

        for (path in candidates) {
            val jsonString = readAsset(path) ?: continue
            val result = FrequencyPatternParser.parseToHapticPattern(jsonString)
            if (result.isSuccess) {
                val pattern = result.getOrThrow()
                val durationMs = pattern.videoDurationMs ?: pattern.events.maxOfOrNull { it.startTimeMs + it.durationMs } ?: 0L
                com.haptix.app.util.HaptiXLog.d(
                    """
                    HAPTIX_HAPTIC_ASSET:
                    video=$videoId
                    asset=$path
                    fileName=${path.substringAfterLast('/')}
                    events=${pattern.events.size}
                    durationMs=$durationMs
                    source=GENERATED
                    """.trimIndent()
                )
                return pattern.copy(source = HapticSource.GENERATED)
            }
        }
        return null
    }

    private fun readAsset(path: String): String? {
        if (assetReader != null) {
            return assetReader.invoke(path)
        }
        val ctx = context ?: return null
        return try {
            ctx.assets.open(path).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }
}
