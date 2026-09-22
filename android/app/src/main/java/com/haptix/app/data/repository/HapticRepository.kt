package com.haptix.app.data.repository

import android.content.Context
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.parser.FrequencyPatternParser

/**
 * Repository interface for loading video-specific haptic pattern configurations.
 */
interface HapticRepository {
    /**
     * Loads the haptic pattern associated with a given video id from raw JSON resources.
     */
    suspend fun loadHapticPatternForVideo(videoId: String): Result<HapticPattern>
}

/**
 * Concrete implementation of [HapticRepository] reading researched frequency patterns from assets.
 * Falls back to an empty pattern gracefully if context is null or files are unavailable.
 */
class DefaultHapticRepository(
    private val context: Context? = null
) : HapticRepository {

    override suspend fun loadHapticPatternForVideo(videoId: String): Result<HapticPattern> {
        val appContext = context ?: return Result.success(
            HapticPattern(videoId = videoId, version = "1.0", events = emptyList())
        )

        // Candidate asset filenames in priority order
        val candidates = listOf(
            "haptics/haptics_${videoId}.json",
            "haptics/${videoId}.json",
            "haptics/android_frequency_stimulus.json"
        )

        for (path in candidates) {
            try {
                appContext.assets.open(path).use { stream ->
                    val parseResult = FrequencyPatternParser.parseInputStream(stream)
                    if (parseResult.isSuccess) {
                        val frequencyPattern = parseResult.getOrThrow()
                        return Result.success(frequencyPattern.toHapticPattern().copy(videoId = videoId))
                    }
                }
            } catch (_: Exception) {
                // Continue to next candidate
            }
        }

        // Default empty pattern if no asset found or parsing fails
        return Result.success(
            HapticPattern(
                videoId = videoId,
                version = "1.0",
                events = emptyList()
            )
        )
    }
}
