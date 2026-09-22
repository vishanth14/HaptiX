package com.haptix.app.data.repository

import com.haptix.app.data.model.VideoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repository interface for retrieving experimental video items.
 */
interface VideoRepository {
    fun getVideos(): Flow<List<VideoItem>>
    suspend fun getVideoById(id: String): VideoItem?
}

/**
 * Placeholder implementation of [VideoRepository] providing research test stimuli.
 */
class DefaultVideoRepository : VideoRepository {
    private val sampleVideos = listOf(
        VideoItem(
            id = "video_01",
            title = "Experimental Clip 1: Low-Frequency Dynamics",
            description = "Subtle ambient rumble synchronization assessment.",
            durationMs = 30_000L,
            videoResUri = "sample_video_01",
            hapticConfigResName = "haptics_video_01"
        ),
        VideoItem(
            id = "video_02",
            title = "Experimental Clip 2: High-Frequency Transients",
            description = "Sharp transient impulse synchronization assessment.",
            durationMs = 45_000L,
            videoResUri = "sample_video_02",
            hapticConfigResName = "haptics_video_02"
        ),
        VideoItem(
            id = "video_03",
            title = "Experimental Clip 3: Modulated Rhythms",
            description = "Combined pulse sequences and continuous pattern modulation.",
            durationMs = 60_000L,
            videoResUri = "sample_video_03",
            hapticConfigResName = "haptics_video_03"
        )
    )

    override fun getVideos(): Flow<List<VideoItem>> = flowOf(sampleVideos)

    override suspend fun getVideoById(id: String): VideoItem? {
        return sampleVideos.find { it.id == id }
    }
}
