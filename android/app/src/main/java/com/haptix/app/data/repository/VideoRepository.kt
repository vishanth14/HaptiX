package com.haptix.app.data.repository

import com.haptix.app.R
import com.haptix.app.data.model.VideoItem
import com.haptix.app.domain.model.CuratedVideo
import com.haptix.app.domain.model.VideoSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Repository interface for retrieving experimental, remote, and curated reference video items.
 */
interface VideoRepository {

    fun getVideos(): Flow<List<VideoItem>>

    suspend fun getVideoById(id: String): VideoItem?

    fun registerVideo(videoItem: VideoItem) {}

    fun getCuratedVideos(): Flow<List<CuratedVideo>> = flowOf(emptyList())
}

/**
 * Default implementation of [VideoRepository] providing:
 *
 * - Remote stimulus registration
 * - Koji paper demonstration
 * - F1 research stimulus
 * - Legacy research samples
 */
class DefaultVideoRepository(
    initialRemoteVideos: List<VideoItem> = emptyList()
) : VideoRepository {

    companion object {

        private val registeredRemoteVideos =
            java.util.concurrent.CopyOnWriteArrayList<VideoItem>()

        /**
         * Globally registers a remote stimulus for playback across
         * view models and navigation sessions.
         */
        fun registerRemoteVideo(videoItem: VideoItem) {

            val existingIndex = registeredRemoteVideos.indexOfFirst {
                it.id.equals(videoItem.id, ignoreCase = true)
            }

            if (existingIndex >= 0) {
                registeredRemoteVideos[existingIndex] = videoItem
            } else {
                registeredRemoteVideos.add(videoItem)
            }
        }

        /**
         * Clears dynamically registered remote stimuli.
         * Useful for test resets.
         */
        fun clearRemoteVideos() {
            registeredRemoteVideos.clear()
        }

        /**
         * Returns all currently registered remote videos.
         */
        fun getRegisteredVideos(): List<VideoItem> {
            return registeredRemoteVideos.toList()
        }
    }

    init {
        for (item in initialRemoteVideos) {
            registerRemoteVideo(item)
        }
    }

    override fun registerVideo(videoItem: VideoItem) {
        registerRemoteVideo(videoItem)
    }

    /**
     * Koji demonstration video.
     */
    private val kojiDemoVideo = VideoItem(
        id = "koji",
        title = "Koji Animation Demo: Multimodal Synchronization",
        description = "Full-length animation sequence featuring synchronized tactile stimuli corresponding to physical impacts, motion accelerations, and narrative climax points.",
        durationMs = 290_134L,
        thumbnailResUri = "koji_thumbnail",
        videoResUri = "koji",
        hapticConfigResName = "koji_haptic_timeline",
        source = VideoSource.Curated(
            "koji",
            "PAPER_PIPELINE_DEMO"
        ),
        category = "PAPER_PIPELINE_DEMO",
        thumbnailResId = R.drawable.koji_thumbnail
    )

    /**
     * F1 research stimulus.
     *
     * NORMAL APP PLAYBACK:
     *
     * Video:
     *     f1_2025_haptic_trailer
     *
     * Haptic timeline:
     *     f1_haptic_timeline.json
     *
     * The Android asset path is:
     *
     * app/src/main/assets/haptics/f1_haptic_timeline.json
     */
    private val f1TrailerVideo = VideoItem(
        id = "f1_2025_haptic_trailer",
        title = "F1 (2025) Official Teaser Trailer",
        description = "Apple Original Films F1 teaser trailer with reconstructed multimodal tactile stimuli including engine rumble, high-speed motion, curb vibration, gear shifts, and cinematic impacts.",
        durationMs = 130_505L,
        thumbnailResUri = "f1_2025_thumbnail",
        videoResUri = "f1_2025_haptic_trailer",

        // Normal F1 runtime haptic configuration.
        hapticConfigResName = "f1_haptic_timeline",

        source = VideoSource.Curated(
            "f1_2025_haptic_trailer",
            "CINEMATIC_RECONSTRUCTION"
        ),
        category = "CINEMATIC_RECONSTRUCTION",
        thumbnailResId = R.drawable.f1_2025_thumbnail
    )

    /**
     * Legacy research sample videos.
     */
    private val legacySampleVideos = listOf(

        VideoItem(
            id = "video_01",
            title = "Experimental Clip 1: Low-Frequency Dynamics",
            description = "Subtle ambient rumble synchronization assessment.",
            durationMs = 30_000L,
            videoResUri = "sample_video_01",
            category = "RESEARCH_STIMULUS"
        ),

        VideoItem(
            id = "video_02",
            title = "Experimental Clip 2: High-Frequency Transients",
            description = "Sharp transient impulse synchronization assessment.",
            durationMs = 45_000L,
            videoResUri = "sample_video_02",
            category = "RESEARCH_STIMULUS"
        ),

        VideoItem(
            id = "video_03",
            title = "Experimental Clip 3: Modulated Rhythms",
            description = "Combined pulse sequences and continuous pattern modulation.",
            durationMs = 60_000L,
            videoResUri = "sample_video_03",
            category = "RESEARCH_STIMULUS"
        )
    )

    /**
     * Main research videos shown in the research library.
     */
    private val researchVideos = listOf(
        f1TrailerVideo,
        kojiDemoVideo
    )

    /**
     * All locally defined videos.
     */
    private val allVideos =
        researchVideos + legacySampleVideos

    /**
     * Returns registered remote videos followed by research videos.
     */
    override fun getVideos(): Flow<List<VideoItem>> {

        val remotes = registeredRemoteVideos.toList()

        return flowOf(
            remotes + researchVideos
        )
    }

    /**
     * Currently no additional curated-video list is exposed.
     */
    override fun getCuratedVideos(): Flow<List<CuratedVideo>> {
        return flowOf(emptyList())
    }

    /**
     * Finds a video by:
     *
     * 1. Registered remote ID
     * 2. Registered remote URL
     * 3. Local video ID
     * 4. Koji/F1 fallback matching
     * 5. HTTP/HTTPS remote URL
     */
    override suspend fun getVideoById(id: String): VideoItem? {

        val remotes = registeredRemoteVideos.toList()

        val found =
            remotes.find {
                it.id.equals(id, ignoreCase = true)
            }
                ?: remotes.find {
                    it.videoUrl.equals(id, ignoreCase = true)
                }
                ?: allVideos.find {
                    it.id.equals(id, ignoreCase = true)
                }
                ?: if (id.contains("koji", ignoreCase = true)) {

                    kojiDemoVideo

                } else if (id.contains("f1", ignoreCase = true)) {

                    f1TrailerVideo

                } else if (
                    id.startsWith("http://", ignoreCase = true) ||
                    id.startsWith("https://", ignoreCase = true)
                ) {

                    VideoItem(
                        id = id,
                        title = "Remote Stimulus",
                        videoUrl = id,
                        hapticUrl = ""
                    )

                } else {
                    null
                }

        return found
    }
}