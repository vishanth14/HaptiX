package com.haptix.app.ui

import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticEventType
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.model.SemanticHapticType
import com.haptix.app.domain.model.HapticSource
import com.haptix.app.domain.model.ProcessingJob
import com.haptix.app.domain.model.ProcessingStatus
import com.haptix.app.domain.model.VideoSource
import com.haptix.app.domain.provider.GeneratedHapticProvider
import com.haptix.app.domain.service.HapticProcessingService
import com.haptix.app.ui.screens.youtube.YouTubeIngestionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeIngestionFlowTest {

    private fun createSamplePattern(videoId: String, eventCount: Int = 3): HapticPattern {
        val events = (0 until eventCount).map { i ->
            HapticEvent(
                id = "evt_$i",
                type = HapticEventType.CONTINUOUS,
                startTimeMs = (i * 1000).toLong(),
                durationMs = 200L,
                semanticType = SemanticHapticType.SMOOTH,
                intensity = 0.7f,
                attackMs = 20L,
                sustainMs = 150L,
                releaseMs = 30L
            )
        }
        return HapticPattern(
            videoId = videoId,
            version = "2.1.0",
            source = HapticSource.GENERATED,
            events = events
        )
    }

    private class FakeProcessingService(
        private val shouldFail: Boolean = false,
        private val failureError: String = "Network timeout"
    ) : HapticProcessingService {

        override suspend fun submitJob(url: String): Result<ProcessingJob> {
            return if (shouldFail) {
                Result.failure(RuntimeException(failureError))
            } else {
                Result.success(
                    ProcessingJob(
                        jobId = "yt_sample123",
                        videoSource = VideoSource.YouTube(url = url, videoId = "sample123"),
                        status = ProcessingStatus.DOWNLOADING,
                        progress = 0.1f,
                        stageMessage = "DOWNLOADING"
                    )
                )
            }
        }

        override suspend fun getJobStatus(jobId: String): Result<ProcessingJob> {
            return if (shouldFail) {
                Result.failure(RuntimeException(failureError))
            } else {
                Result.success(
                    ProcessingJob(
                        jobId = jobId,
                        videoSource = VideoSource.YouTube(url = "https://www.youtube.com/watch?v=sample123", videoId = "sample123"),
                        status = ProcessingStatus.COMPLETED,
                        progress = 1.0f,
                        stageMessage = "READY"
                    )
                )
            }
        }

        override suspend fun getGeneratedPattern(jobId: String): Result<HapticPattern> {
            return if (shouldFail) {
                Result.failure(RuntimeException(failureError))
            } else {
                val events = listOf(
                    HapticEvent(
                        id = "evt_0",
                        type = HapticEventType.TRANSIENT,
                        startTimeMs = 100L,
                        durationMs = 0L,
                        semanticType = SemanticHapticType.HEAVY_IMPACT,
                        intensity = 0.9f
                    )
                )
                Result.success(
                    HapticPattern(
                        videoId = jobId,
                        version = "2.1.0",
                        source = HapticSource.GENERATED,
                        events = events
                    )
                )
            }
        }
    }

    // 1. URL validation tests
    @Test
    fun youTubeViewModel_validatesStandardWatchUrl() {
        val viewModel = YouTubeIngestionViewModel(FakeProcessingService())
        viewModel.onUrlChanged("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        val state = viewModel.uiState.value
        assertTrue(state.isValidUrl)
        assertEquals("dQw4w9WgXcQ", state.extractedVideoId)
        assertNull(state.errorMessage)
    }

    @Test
    fun youTubeViewModel_validatesShortYoutuBeUrl() {
        val viewModel = YouTubeIngestionViewModel(FakeProcessingService())
        viewModel.onUrlChanged("https://youtu.be/dQw4w9WgXcQ")
        val state = viewModel.uiState.value
        assertTrue(state.isValidUrl)
        assertEquals("dQw4w9WgXcQ", state.extractedVideoId)
        assertNull(state.errorMessage)
    }

    @Test
    fun youTubeViewModel_rejectsEmptyUrl() {
        val viewModel = YouTubeIngestionViewModel(FakeProcessingService())
        viewModel.onUrlChanged("")
        viewModel.submitUrl()

        val state = viewModel.uiState.value
        assertFalse(state.isValidUrl)
        assertFalse(state.isProcessing)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun youTubeViewModel_rejectsNonYouTubeUrl() {
        val viewModel = YouTubeIngestionViewModel(FakeProcessingService())
        viewModel.onUrlChanged("https://vimeo.com/12345678")
        viewModel.submitUrl()

        val state = viewModel.uiState.value
        assertFalse(state.isValidUrl)
        assertFalse(state.isProcessing)
        assertNotNull(state.errorMessage)
    }

    // 2. Successful workflow progression
    @Test
    fun youTubeViewModel_executesCompleteProcessingFlow() = runBlocking {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val fakeService = FakeProcessingService()
        val viewModel = YouTubeIngestionViewModel(fakeService, coroutineScope = testScope, pollingIntervalMs = 0L)
        viewModel.onUrlChanged("https://www.youtube.com/watch?v=sample123")
        viewModel.submitUrl()

        val state = viewModel.uiState.value
        assertFalse(state.isProcessing)
        assertTrue(state.isCompleted)
        assertEquals(ProcessingStatus.COMPLETED, state.currentStatus)
        assertEquals(1.0f, state.progress, 0.01f)
        assertNotNull(state.generatedPattern)
        assertEquals("yt_sample123", state.activeJobId)
        assertNull(state.errorMessage)
    }

    // 3. Error handling in processing flow
    @Test
    fun youTubeViewModel_handlesSubmissionFailureGracefully() = runBlocking {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val failureService = FakeProcessingService(shouldFail = true, failureError = "Connection refused")
        val viewModel = YouTubeIngestionViewModel(failureService, coroutineScope = testScope)
        viewModel.onUrlChanged("https://www.youtube.com/watch?v=sample123")
        viewModel.submitUrl()

        val state = viewModel.uiState.value
        assertFalse(state.isProcessing)
        assertFalse(state.isCompleted)
        assertEquals(ProcessingStatus.FAILED, state.currentStatus)
        assertEquals("Connection refused", state.errorMessage)
        assertNull(state.generatedPattern)
    }

    // 4. Reset action
    @Test
    fun youTubeViewModel_resetClearsState() = runBlocking {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = YouTubeIngestionViewModel(FakeProcessingService(), coroutineScope = testScope)
        viewModel.onUrlChanged("https://www.youtube.com/watch?v=sample123")
        viewModel.submitUrl()

        viewModel.reset()
        val resetState = viewModel.uiState.value
        assertEquals("", resetState.urlInput)
        assertFalse(resetState.isProcessing)
        assertFalse(resetState.isCompleted)
        assertNull(resetState.generatedPattern)
    }

    // 5. GeneratedHapticProvider tests
    @Test
    fun generatedHapticProvider_supportsDynamicRegistration() = runBlocking {
        val provider = GeneratedHapticProvider()
        val samplePattern = createSamplePattern("yt_dynamic_001", 5)

        provider.registerPattern(samplePattern)

        val retrieved = provider.getPattern("yt_dynamic_001")
        assertNotNull(retrieved)
        assertEquals("yt_dynamic_001", retrieved?.videoId)
        assertEquals(5, retrieved?.events?.size)
        assertEquals(HapticSource.GENERATED, retrieved?.source)
    }

    @Test
    fun generatedHapticProvider_retrievesByPrefixOrCleanId() = runBlocking {
        val provider = GeneratedHapticProvider()
        val samplePattern = createSamplePattern("sample123", 2)
        provider.registerPattern(samplePattern)

        val retrieved = provider.getPattern("sample123")
        assertNotNull(retrieved)
        assertEquals(2, retrieved?.events?.size)
    }
}
