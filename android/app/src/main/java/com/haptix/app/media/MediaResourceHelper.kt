package com.haptix.app.media

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.haptix.app.data.model.VideoItem

/**
 * Utility helper to resolve local raw resources and build Media3 [MediaItem] instances
 * for experimental video playback.
 */
object MediaResourceHelper {

    /**
     * Resolves a local raw resource or URI string to an actionable Android [Uri].
     * Checks `app/src/main/res/raw` first by resource identifier name.
     */
    fun resolveVideoUri(context: Context, videoResUri: String): Uri? {
        if (videoResUri.isBlank()) return null

        // 1. Direct standard URI schemas (file, http, content, android.resource)
        if (videoResUri.startsWith("http://") ||
            videoResUri.startsWith("https://") ||
            videoResUri.startsWith("file://") ||
            videoResUri.startsWith("content://") ||
            videoResUri.startsWith("android.resource://")
        ) {
            return Uri.parse(videoResUri)
        }

        // 2. Resource name resolution against app/src/main/res/raw (e.g. "sample_video_01")
        val cleanName = videoResUri
            .substringAfterLast("/")
            .substringBefore(".")
            .trim()

        val rawId = context.resources.getIdentifier(cleanName, "raw", context.packageName)
        if (rawId != 0) {
            return Uri.parse("android.resource://${context.packageName}/$rawId")
        }

        return null
    }

    /**
     * Constructs a Media3 [MediaItem] with attached metadata for playback.
     */
    fun buildMediaItem(context: Context, videoItem: VideoItem): MediaItem? {
        val uri = resolveVideoUri(context, videoItem.videoResUri) ?: return null

        return MediaItem.Builder()
            .setMediaId(videoItem.id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(videoItem.title)
                    .setDescription(videoItem.description)
                    .build()
            )
            .build()
    }
}
