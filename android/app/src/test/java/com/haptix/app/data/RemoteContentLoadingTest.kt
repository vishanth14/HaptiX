package com.haptix.app.data

import com.haptix.app.data.model.VideoItem
import com.haptix.app.data.repository.DefaultHapticRepository
import com.haptix.app.data.repository.DefaultVideoRepository
import com.haptix.app.data.repository.HapticRemoteDownloader
import com.haptix.app.domain.model.VideoSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RemoteContentLoadingTest {

    @After
    fun tearDown() {
        DefaultVideoRepository.clearRemoteVideos()
    }

    // 1. VideoItem URL Model Test
    @Test
    fun videoItem_supportsRemoteVideoAndHapticUrls() {
        val remoteStimulus = VideoItem(
            id = "senior_stimulus_01",
            title = "Research Trial 01",
            videoUrl = "https://cdn.haptix.org/trials/video_01.mp4",
            hapticUrl = "https://cdn.haptix.org/trials/haptics_01.json"
        )

        assertEquals("senior_stimulus_01", remoteStimulus.id)
        assertEquals("Research Trial 01", remoteStimulus.title)
        assertEquals("https://cdn.haptix.org/trials/video_01.mp4", remoteStimulus.videoUrl)
        assertEquals("https://cdn.haptix.org/trials/haptics_01.json", remoteStimulus.hapticUrl)
        assertEquals("https://cdn.haptix.org/trials/video_01.mp4", remoteStimulus.resolvedVideoUri)
        assertTrue(remoteStimulus.source is VideoSource.Url)
        assertEquals(VideoSource.Type.URL, remoteStimulus.source.type)
    }

    // 2. VideoItem Backward Compatibility with Local Assets
    @Test
    fun videoItem_preservesBackwardCompatibilityWithLocalAssets() {
        val localVideo = VideoItem(
            id = "f1_2025_haptic_trailer",
            title = "F1 Trailer",
            durationMs = 130000L,
            videoResUri = "f1_2025_haptic_trailer",
            hapticConfigResName = "f1_haptic_timeline"
        )

        assertEquals("f1_2025_haptic_trailer", localVideo.resolvedVideoUri)
        assertEquals("", localVideo.hapticUrl)
        assertTrue(localVideo.source is VideoSource.Local)
    }

    // 3. VideoRepository Registration & Resolution
    @Test
    fun videoRepository_registersAndRetrievesRemoteStimuli() = runBlocking {
        val repo = DefaultVideoRepository()
        val remoteVideo = VideoItem(
            id = "remote_exp_42",
            title = "Dynamic Remote Experiment",
            videoUrl = "https://cdn.example.com/stream.mp4",
            hapticUrl = "https://cdn.example.com/stream_haptics.json"
        )

        repo.registerVideo(remoteVideo)

        val retrieved = repo.getVideoById("remote_exp_42")
        assertNotNull(retrieved)
        assertEquals("remote_exp_42", retrieved?.id)
        assertEquals("https://cdn.example.com/stream.mp4", retrieved?.videoUrl)
        assertEquals("https://cdn.example.com/stream_haptics.json", retrieved?.hapticUrl)

        // Verify local videos are preserved
        val f1 = repo.getVideoById("f1_2025_haptic_trailer")
        assertNotNull(f1)
        val koji = repo.getVideoById("koji")
        assertNotNull(koji)

        // Verify list contains registered remote video
        val videos = repo.getVideos().first()
        assertTrue(videos.any { it.id == "remote_exp_42" })
    }

    // 4. Remote Haptic JSON Loading & Parsing (Events Schema)
    @Test
    fun hapticRepository_loadsAndParsesRemoteHapticJsonEvents() = runBlocking {
        val sampleJson = """
            {
              "videoId": "senior_sample",
              "version": "1.0",
              "events": [
                {
                  "startTimeMs": 500,
                  "durationMs": 300,
                  "intensity": 0.8,
                  "sharpness": 0.6,
                  "semanticType": "SMOOTH"
                },
                {
                  "startTimeMs": 1500,
                  "durationMs": 100,
                  "intensity": 0.95,
                  "sharpness": 0.9,
                  "semanticType": "SHARP"
                }
              ]
            }
        """.trimIndent()

        val mockDownloader = object : HapticRemoteDownloader {
            override suspend fun fetchJsonText(url: String): Result<String> {
                return if (url == "https://example.com/haptics.json") {
                    Result.success(sampleJson)
                } else {
                    Result.failure(IOException("HTTP 404 Not Found"))
                }
            }
        }

        val repository = DefaultHapticRepository(remoteDownloader = mockDownloader)
        val result = repository.loadHapticPatternFromUrl("https://example.com/haptics.json", "senior_sample")

        assertTrue(result.isSuccess)
        val pattern = result.getOrThrow()
        assertEquals("senior_sample", pattern.videoId)
        assertEquals(2, pattern.events.size)
        assertEquals(500L, pattern.events[0].startTimeMs)
        assertEquals(300L, pattern.events[0].durationMs)
        assertEquals(0.8f, pattern.events[0].intensity, 0.001f)
        assertEquals(1500L, pattern.events[1].startTimeMs)
    }

    // 5. Remote Haptic JSON Loading & Parsing (Points Schema)
    @Test
    fun hapticRepository_loadsAndParsesRemoteHapticJsonPoints() = runBlocking {
        val samplePointsJson = """
            {
              "videoId": "points_sample",
              "points": [
                {
                  "startTimeMs": 1000,
                  "durationMs": 150,
                  "frequencyHz": 180.0,
                  "amplitude": 0.75
                }
              ]
            }
        """.trimIndent()

        val mockDownloader = object : HapticRemoteDownloader {
            override suspend fun fetchJsonText(url: String): Result<String> {
                return Result.success(samplePointsJson)
            }
        }

        val repository = DefaultHapticRepository(remoteDownloader = mockDownloader)
        val result = repository.loadHapticPatternFromUrl("https://example.com/points.json", "points_sample")

        assertTrue(result.isSuccess)
        val pattern = result.getOrThrow()
        assertEquals(1, pattern.events.size)
        assertEquals(1000L, pattern.events[0].startTimeMs)
        assertEquals(150L, pattern.events[0].durationMs)
    }

    // 6. Remote Haptic Loading Failure: Network Error
    @Test
    fun hapticRepository_handlesNetworkFailureGracefully() = runBlocking {
        val mockDownloader = object : HapticRemoteDownloader {
            override suspend fun fetchJsonText(url: String): Result<String> {
                return Result.failure(IOException("Failed to connect to host"))
            }
        }

        val repository = DefaultHapticRepository(remoteDownloader = mockDownloader)
        val result = repository.loadHapticPatternFromUrl("https://bad.host/haptics.json", "fail_test")

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertNotNull(ex)
        assertTrue(ex is IOException)
    }

    // 7. Remote Haptic Loading Failure: Empty Response
    @Test
    fun hapticRepository_handlesEmptyJsonResponse() = runBlocking {
        val mockDownloader = object : HapticRemoteDownloader {
            override suspend fun fetchJsonText(url: String): Result<String> {
                return Result.success("   ")
            }
        }

        val repository = DefaultHapticRepository(remoteDownloader = mockDownloader)
        val result = repository.loadHapticPatternFromUrl("https://example.com/empty.json", "empty_test")

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue(ex is IllegalArgumentException)
    }

    // 8. Remote Haptic Loading Failure: Malformed JSON
    @Test
    fun hapticRepository_handlesMalformedJsonGracefully() = runBlocking {
        val mockDownloader = object : HapticRemoteDownloader {
            override suspend fun fetchJsonText(url: String): Result<String> {
                return Result.success("Not a JSON document {{{ invalid syntax")
            }
        }

        val repository = DefaultHapticRepository(remoteDownloader = mockDownloader)
        val result = repository.loadHapticPatternFromUrl("https://example.com/corrupt.json", "corrupt_test")

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
    }

    // 9. Load Pattern For VideoItem Unified Dispatcher
    @Test
    fun hapticRepository_unifiedDispatcherRoutesCorrectly() = runBlocking {
        var remoteCalled = false
        val mockDownloader = object : HapticRemoteDownloader {
            override suspend fun fetchJsonText(url: String): Result<String> {
                remoteCalled = true
                return Result.success("""{"videoId":"test","events":[]}""")
            }
        }

        val repository = DefaultHapticRepository(remoteDownloader = mockDownloader)

        // Item with hapticUrl
        val remoteItem = VideoItem(
            id = "test_item",
            title = "Test",
            videoUrl = "https://cdn.example.com/vid.mp4",
            hapticUrl = "https://cdn.example.com/hap.json"
        )
        val resRemote = repository.loadPatternForVideoItem(remoteItem)
        assertTrue(resRemote.isSuccess)
        assertTrue(remoteCalled)

        // Item without hapticUrl falls back to local search
        val localItem = VideoItem(
            id = "unknown_local",
            title = "Test Local",
            videoResUri = "sample"
        )
        val resLocal = repository.loadPatternForVideoItem(localItem)
        assertTrue(resLocal.isSuccess) // returns fallback empty pattern
    }
}
