package com.haptix.app.domain.provider

import android.content.Context
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.parser.FrequencyPatternParser
import com.haptix.app.domain.model.HapticSource

/**
 * Provider supplying legacy manually-authored haptic patterns (e.g. historical Koji keypoints).
 * Explicitly marks all loaded patterns as [HapticSource.LEGACY_MANUAL].
 */
class LegacyManualHapticProvider(
    private val context: Context? = null,
    private val assetReader: ((String) -> String?)? = null
) : HapticSourceProvider {

    override val source: HapticSource = HapticSource.LEGACY_MANUAL

    override suspend fun getPattern(videoId: String): HapticPattern? {
        val isF1OrKoji = videoId.contains("f1", ignoreCase = true) || videoId.contains("koji", ignoreCase = true)
        val candidates = when {
            isF1OrKoji -> listOf(
                "haptics/${videoId}_haptics.json",
                "haptics/haptics_${videoId}.json",
                "haptics/${videoId}.json"
            )
            else -> listOf(
                "haptics/${videoId}_haptics.json",
                "haptics/haptics_${videoId}.json",
                "haptics/${videoId}.json"
            )
        }

        for (path in candidates) {
            val jsonString = readAsset(path) ?: continue
            val result = FrequencyPatternParser.parseJson(jsonString)
            if (result.isSuccess) {
                val pattern = result.getOrThrow().toHapticPattern()
                return pattern.copy(
                    videoId = videoId,
                    source = HapticSource.LEGACY_MANUAL
                )
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
