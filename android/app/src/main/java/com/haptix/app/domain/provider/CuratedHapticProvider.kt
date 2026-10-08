package com.haptix.app.domain.provider

import android.content.Context
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.parser.FrequencyPatternParser
import com.haptix.app.domain.model.HapticSource

/**
 * Provider supplying curated, pre-validated reference tactile stimuli (e.g. F1 onboard, benchmarks).
 */
class CuratedHapticProvider(
    private val context: Context? = null,
    private val assetReader: ((String) -> String?)? = null
) : HapticSourceProvider {

    override val source: HapticSource = HapticSource.CURATED

    override suspend fun getPattern(videoId: String): HapticPattern? {
        return getPattern(videoId, null)
    }

    override suspend fun getPattern(videoId: String, hapticConfigResName: String?): HapticPattern? {
        val cleanId = videoId.removePrefix("yt_").trim()
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
                    source=CURATED
                    """.trimIndent()
                )
                return pattern.copy(source = HapticSource.CURATED)
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
