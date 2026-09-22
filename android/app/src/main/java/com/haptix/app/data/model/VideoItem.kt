package com.haptix.app.data.model

/**
 * Metadata representing an experimental video item.
 *
 * @param id Unique video identifier.
 * @param title Video title.
 * @param durationMs Playback duration in milliseconds.
 * @param thumbnailResUri Optional URI or resource name for the video thumbnail.
 * @param videoResUri URI or asset resource name for video content playback.
 * @param description Brief description of experimental stimuli.
 * @param hapticConfigResName Associated haptic pattern resource identifier.
 */
data class VideoItem(
    val id: String,
    val title: String,
    val durationMs: Long,
    val thumbnailResUri: String? = null,
    val videoResUri: String = "",
    val description: String = "",
    val hapticConfigResName: String = ""
)
