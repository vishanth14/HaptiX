package com.haptix.app.domain.model

/**
 * Platform-independent categorization of video origin.
 *
 * Supported origins:
 * - [LOCAL]: Video stored locally on the device (e.g. bundled in app assets or local filesystem).
 * - [CURATED]: High-fidelity reference benchmark video (e.g. F1 onboard telemetry, research baseline).
 * - [YOUTUBE]: Remote video originating from a YouTube stream or URL.
 */
sealed class VideoSource {
    abstract val identifier: String

    /**
     * Local device asset or filesystem path.
     */
    data class Local(
        val assetPath: String,
        override val identifier: String = assetPath
    ) : VideoSource() {
        init {
            require(assetPath.isNotBlank()) { "assetPath must not be blank" }
        }
    }

    /**
     * Curated reference stimulus (e.g. F1 curated clips, Koji master demonstration).
     */
    data class Curated(
        val referenceId: String,
        val category: String = "REFERENCE",
        override val identifier: String = referenceId
    ) : VideoSource() {
        init {
            require(referenceId.isNotBlank()) { "referenceId must not be blank" }
        }
    }

    /**
     * Remote YouTube video stream for automated pipeline processing.
     */
    data class YouTube(
        val url: String,
        val videoId: String = extractYouTubeId(url),
        override val identifier: String = videoId
    ) : VideoSource() {
        init {
            require(url.isNotBlank()) { "YouTube url must not be blank" }
            require(videoId.isNotBlank()) { "Extracted videoId must not be blank" }
        }

        companion object {
            fun extractYouTubeId(url: String): String {
                val clean = url.trim()
                return when {
                    clean.contains("v=") -> clean.substringAfter("v=").substringBefore("&").substringBefore("?")
                    clean.contains("youtu.be/") -> clean.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
                    clean.contains("shorts/") -> clean.substringAfter("shorts/").substringBefore("?").substringBefore("&")
                    else -> clean
                }
            }
        }
    }

    /**
     * Remote direct video URL (e.g. MP4 hosted on HTTP/HTTPS).
     */
    data class Url(
        val url: String,
        override val identifier: String = url
    ) : VideoSource() {
        init {
            require(url.isNotBlank()) { "url must not be blank" }
        }
    }

    enum class Type {
        LOCAL,
        CURATED,
        YOUTUBE,
        URL
    }

    val type: Type
        get() = when (this) {
            is Local -> Type.LOCAL
            is Curated -> Type.CURATED
            is YouTube -> Type.YOUTUBE
            is Url -> Type.URL
        }
}

/**
 * Platform-independent metadata describing a video stimulus.
 *
 * Free of Android-specific (Context, Uri, ExoPlayer) or iOS-specific classes.
 */
data class VideoMetadata(
    val videoId: String,
    val title: String,
    val durationMs: Long,
    val width: Int = 0,
    val height: Int = 0,
    val fps: Float = 0.0f,
    val format: String = "mp4",
    val source: VideoSource = VideoSource.Local(videoId),
    val description: String = ""
) {
    init {
        require(videoId.isNotBlank()) { "videoId must not be blank" }
        require(durationMs >= 0L) { "durationMs must be non-negative, was $durationMs" }
        require(width >= 0) { "width must be non-negative" }
        require(height >= 0) { "height must be non-negative" }
        require(fps >= 0.0f) { "fps must be non-negative" }
    }

    val durationSeconds: Float get() = durationMs / 1000.0f
    val aspectRatio: Float get() = if (height > 0) width.toFloat() / height.toFloat() else 0.0f
}
