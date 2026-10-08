package com.haptix.app.data.repository

import android.content.Context
import com.haptix.app.data.model.HapticFrequencyPattern
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.domain.model.HapticSource
import com.haptix.app.domain.provider.CuratedHapticProvider
import com.haptix.app.domain.provider.GeneratedHapticProvider
import com.haptix.app.domain.provider.HapticSourceProvider
import com.haptix.app.domain.provider.LegacyManualHapticProvider
import com.haptix.app.platform.android.AndroidHapticConverter

/**
 * Repository interface for loading video-specific haptic pattern configurations.
 *
 * Supports decoupled tactile sources:
 * - Remote HTTP/HTTPS URL streams
 * - [HapticSource.CURATED]
 * - [HapticSource.GENERATED]
 * - [HapticSource.LEGACY_MANUAL]
 */
interface HapticRepository {
    /**
     * Loads the frequency-calibrated haptic pattern associated with a given video id from assets.
     */
    suspend fun loadFrequencyPatternForVideo(videoId: String): Result<HapticFrequencyPattern> {
        return loadHapticPatternForVideo(videoId).map { pattern ->
            AndroidHapticConverter.toAndroidFrequencyPattern(pattern)
        }
    }

    /**
     * Loads the domain haptic pattern associated with a given video id from raw JSON resources.
     */
    suspend fun loadHapticPatternForVideo(videoId: String): Result<HapticPattern>

    /**
     * Loads the domain haptic pattern associated with a given video id and optional configuration resource name.
     */
    suspend fun loadHapticPatternForVideo(videoId: String, hapticConfigResName: String? = null): Result<HapticPattern> = loadHapticPatternForVideo(videoId)

    /**
     * Retrieves and parses a haptic pattern directly from a remote HTTP/HTTPS URL.
     */
    suspend fun loadHapticPatternFromUrl(url: String, videoId: String = ""): Result<HapticPattern> = Result.failure(
        UnsupportedOperationException("Remote URL haptic loading is not supported by this repository implementation")
    )

    /**
     * Unified resolver that dispatches to [loadHapticPatternFromUrl] if [com.haptix.app.data.model.VideoItem.hapticUrl] is present,
     * or falls back to [loadHapticPatternForVideo] for local assets.
     */
    suspend fun loadPatternForVideoItem(videoItem: com.haptix.app.data.model.VideoItem): Result<HapticPattern> {
        return if (videoItem.hapticUrl.isNotBlank()) {
            loadHapticPatternFromUrl(videoItem.hapticUrl, videoItem.id)
        } else {
            loadHapticPatternForVideo(videoItem.id, videoItem.hapticConfigResName)
        }
    }

    /**
     * Configures the preferred haptic source (or null for auto-prioritized resolution).
     */
    fun setPreferredSource(source: HapticSource?) {}

    /**
     * Returns the active source of the most recently loaded pattern.
     */
    fun getActiveSource(): HapticSource = HapticSource.LEGACY_MANUAL
}

/**
 * Network abstraction for fetching raw haptic JSON strings over HTTP/HTTPS.
 * Enables deterministic testing without live internet dependencies.
 */
interface HapticRemoteDownloader {
    suspend fun fetchJsonText(url: String): Result<String>
}

/**
 * Standard Android/JVM HTTP client implementation using [java.net.HttpURLConnection].
 */
class DefaultHapticRemoteDownloader : HapticRemoteDownloader {
    override suspend fun fetchJsonText(url: String): Result<String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (url.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Haptic URL cannot be blank"))
        }
        val cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://", ignoreCase = true) && !cleanUrl.startsWith("https://", ignoreCase = true)) {
            return@withContext Result.failure(IllegalArgumentException("Invalid URL schema for haptic resource: $cleanUrl (expected http:// or https://)"))
        }

        var connection: java.net.HttpURLConnection? = null
        try {
            val endpoint = java.net.URL(cleanUrl)
            connection = (endpoint.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json, text/plain, */*")
                setRequestProperty("User-Agent", "HaptiX-Android/1.0")
            }

            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                return@withContext Result.failure(
                    java.io.IOException("HTTP request to haptic URL failed with status code $statusCode")
                )
            }

            val content = connection.inputStream.bufferedReader().use { it.readText() }
            if (content.isBlank()) {
                return@withContext Result.failure(
                    java.io.IOException("Haptic URL returned empty response body from $cleanUrl")
                )
            }
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}

/**
 * Concrete implementation of [HapticRepository] orchestrating prioritized [HapticSourceProvider]s
 * and remote URL haptic pattern retrieval.
 */
class DefaultHapticRepository(
    private val context: Context? = null,
    private val providers: List<HapticSourceProvider> = listOf(
        GeneratedHapticProvider(context),
        CuratedHapticProvider(context),
        LegacyManualHapticProvider(context)
    ),
    private val remoteDownloader: HapticRemoteDownloader = DefaultHapticRemoteDownloader()
) : HapticRepository {

    private var preferredSource: HapticSource? = null
    private var lastActiveSource: HapticSource = HapticSource.LEGACY_MANUAL

    override fun setPreferredSource(source: HapticSource?) {
        this.preferredSource = source
    }

    override fun getActiveSource(): HapticSource = lastActiveSource

    override suspend fun loadHapticPatternFromUrl(url: String, videoId: String): Result<HapticPattern> {
        val downloadResult = remoteDownloader.fetchJsonText(url)
        if (downloadResult.isFailure) {
            val err = downloadResult.exceptionOrNull() ?: java.io.IOException("Failed to download haptic data from $url")
            com.haptix.app.util.HaptiXLog.e("HAPTIC_URL_DOWNLOAD_FAILED: url=$url, error=${err.message}")
            return Result.failure(err)
        }

        val jsonString = downloadResult.getOrThrow()
        if (jsonString.isBlank()) {
            val err = IllegalArgumentException("Haptic JSON content at $url is empty")
            com.haptix.app.util.HaptiXLog.e("HAPTIC_URL_EMPTY_BODY: url=$url")
            return Result.failure(err)
        }

        val parseResult = com.haptix.app.data.parser.FrequencyPatternParser.parseToHapticPattern(jsonString)
        if (parseResult.isFailure) {
            val err = parseResult.exceptionOrNull() ?: IllegalArgumentException("Failed to parse remote haptic JSON from $url")
            com.haptix.app.util.HaptiXLog.e("HAPTIC_URL_PARSE_FAILED: url=$url, error=${err.message}")
            return Result.failure(err)
        }

        val parsed = parseResult.getOrThrow()
        val effective = if (videoId.isNotBlank() && (parsed.videoId.isBlank() || parsed.videoId == "unspecified_video")) {
            parsed.copy(videoId = videoId)
        } else {
            parsed
        }
        lastActiveSource = effective.source
        com.haptix.app.util.HaptiXLog.d("HAPTIC_URL_LOADED: url=$url, events=${effective.events.size}, source=${effective.source}")
        return Result.success(effective)
    }

    override suspend fun loadHapticPatternForVideo(videoId: String): Result<HapticPattern> {
        return loadHapticPatternForVideo(videoId, null)
    }

    override suspend fun loadHapticPatternForVideo(videoId: String, hapticConfigResName: String?): Result<HapticPattern> {
        // 1. If explicit preferred source is set, query that provider first
        val targetSource = preferredSource
        if (targetSource != null) {
            val provider = providers.find { it.source == targetSource }
            val pattern = provider?.getPattern(videoId, hapticConfigResName) ?: provider?.getPattern(videoId)
            if (pattern != null) {
                lastActiveSource = pattern.source
                return Result.success(pattern)
            }
        }

        // 2. Query all providers in prioritized sequence
        for (provider in providers) {
            val pattern = provider.getPattern(videoId, hapticConfigResName) ?: provider.getPattern(videoId)
            if (pattern != null) {
                lastActiveSource = pattern.source
                if (videoId.contains("f1", ignoreCase = true)) {
                    com.haptix.app.util.HaptiXLog.d("F1:\nloaded events = ${pattern.events.size}")
                } else if (videoId.contains("koji", ignoreCase = true)) {
                    com.haptix.app.util.HaptiXLog.d("Koji:\nloaded events = ${pattern.events.size}")
                }
                return Result.success(pattern)
            }
        }

        // 3. Fallback: empty pattern
        val emptyPattern = HapticPattern(
            videoId = videoId,
            source = HapticSource.LEGACY_MANUAL,
            events = emptyList()
        )
        lastActiveSource = HapticSource.LEGACY_MANUAL
        return Result.success(emptyPattern)
    }

    override suspend fun loadFrequencyPatternForVideo(videoId: String): Result<HapticFrequencyPattern> {
        return loadHapticPatternForVideo(videoId).map { pattern ->
            AndroidHapticConverter.toAndroidFrequencyPattern(pattern)
        }
    }
}
