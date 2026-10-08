package com.haptix.app.domain.provider

import com.haptix.app.data.model.HapticPattern
import com.haptix.app.domain.model.HapticSource

/**
 * Interface for components capable of supplying a platform-independent [HapticPattern].
 */
interface HapticSourceProvider {
    /**
     * The methodology/origin category supported by this provider.
     */
    val source: HapticSource

    /**
     * Resolves the [HapticPattern] for the given video identifier, or null if not available from this source.
     */
    suspend fun getPattern(videoId: String): HapticPattern?

    /**
     * Resolves the [HapticPattern] for the given video identifier with an optional explicit haptic configuration resource name.
     */
    suspend fun getPattern(videoId: String, hapticConfigResName: String? = null): HapticPattern? = getPattern(videoId)
}
