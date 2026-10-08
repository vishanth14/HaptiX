package com.haptix.app.domain.model

import com.haptix.app.data.model.VideoItem

/**
 * Representation of a verified, high-fidelity reference video with precomputed haptics.
 *
 * Preserves platform-independence so Android, future iOS (AHAP), and evaluation harnesses
 * share identical reference metadata.
 *
 * @param id Unique reference identifier (e.g. "f1_reference_01").
 * @param title Descriptive title.
 * @param description Detailed contextual description of the reference scenario.
 * @param durationMs Playback duration in milliseconds.
 * @param localVideoResource Local Android raw resource or URI identifier.
 * @param thumbnailResource Optional thumbnail resource identifier.
 * @param category Racing/reference scene category (e.g. "HIGH_SPEED_ACCELERATION", "HARD_BRAKING").
 * @param source Origin descriptor ([VideoSource.CURATED]).
 * @param hapticPatternResource Associated haptic pattern resource asset name.
 * @param validationStatus Empirical validation status ("VALID").
 * @param provenance Scientific or engineering provenance of the reference data.
 * @param version Format version descriptor.
 */
data class CuratedVideo(
    val id: String,
    val title: String,
    val description: String,
    val durationMs: Long,
    val localVideoResource: String,
    val thumbnailResource: String? = null,
    val category: String,
    val source: VideoSource = VideoSource.Curated(id, category),
    val hapticPatternResource: String,
    val validationStatus: String = "VALID",
    val provenance: String,
    val version: String = "1.0"
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(title.isNotBlank()) { "title must not be blank" }
        require(durationMs > 0L) { "durationMs must be positive, was $durationMs" }
    }

    /**
     * Converts to standard [VideoItem] for seamless UI and playback consumption.
     */
    fun toVideoItem(): VideoItem = VideoItem(
        id = id,
        title = title,
        durationMs = durationMs,
        thumbnailResUri = thumbnailResource,
        videoResUri = localVideoResource,
        description = description,
        hapticConfigResName = hapticPatternResource,
        source = source,
        category = category
    )
}
