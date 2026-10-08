package com.haptix.app.data.model

import com.haptix.app.domain.model.VideoSource

/**
 * Metadata representing an experimental video item.
 *
 * @param id Unique video identifier.
 * @param title Video title.
 * @param durationMs Playback duration in milliseconds (0L if unknown prior to stream preparation).
 * @param thumbnailResUri Optional URI or resource name for the video thumbnail.
 * @param videoResUri URI or asset resource name for video content playback.
 * @param description Brief description of experimental stimuli.
 * @param hapticConfigResName Associated local haptic pattern resource identifier.
 * @param videoUrl Direct URL (HTTP/HTTPS) or URI for video stream playback. Defaults to [videoResUri].
 * @param hapticUrl Direct URL (HTTP/HTTPS) to remote haptic JSON pattern dataset.
 * @param source Origin descriptor ([VideoSource.Local], [VideoSource.Curated], [VideoSource.Url], or [VideoSource.YouTube]).
 * @param category Contextual category (e.g. "HIGH_SPEED_ACCELERATION", "DEMO", "REFERENCE", "REMOTE_STIMULUS").
 */
data class VideoItem(
    val id: String,
    val title: String,
    val durationMs: Long = 0L,
    val thumbnailResUri: String? = null,
    val videoResUri: String = "",
    val description: String = "",
    val hapticConfigResName: String = "",
    val videoUrl: String = videoResUri,
    val hapticUrl: String = "",
    val source: VideoSource = when {
        videoUrl.startsWith("http://") || videoUrl.startsWith("https://") -> VideoSource.Url(videoUrl)
        else -> VideoSource.Local(videoResUri.ifBlank { id })
    },
    val category: String = "DEMO",
    val thumbnailResId: Int? = null
) {
    /**
     * Resolves the primary playback URL or URI.
     * Prefers [videoUrl] if not blank, otherwise falls back to [videoResUri].
     */
    val resolvedVideoUri: String
        get() = videoUrl.ifBlank { videoResUri }

    /**
     * Secondary constructor providing the clean senior-defined stimulus contract:
     * VideoItem(id, title, videoUrl, hapticUrl)
     */
    constructor(
        id: String,
        title: String,
        videoUrl: String,
        hapticUrl: String,
        durationMs: Long = 0L,
        thumbnailResUri: String? = null,
        description: String = "",
        category: String = "REMOTE_STIMULUS",
        thumbnailResId: Int? = null
    ) : this(
        id = id,
        title = title,
        durationMs = durationMs,
        thumbnailResUri = thumbnailResUri,
        videoResUri = videoUrl,
        description = description,
        hapticConfigResName = "",
        videoUrl = videoUrl,
        hapticUrl = hapticUrl,
        source = VideoSource.Url(videoUrl),
        category = category,
        thumbnailResId = thumbnailResId
    )
}
