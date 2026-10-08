package com.haptix.app.ui

import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.model.VideoItem
import com.haptix.app.data.repository.HapticRepository
import com.haptix.app.data.repository.VideoRepository
import com.haptix.app.haptics.HapticCapabilityLevel
import com.haptix.app.haptics.HapticEngine
import com.haptix.app.haptics.HapticSynchronizer
import com.haptix.app.ui.screens.video.VideoPlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlayerViewModelSyncTest {

    private class FakeHapticEngine(private val supported: Boolean = true) : HapticEngine {
        val playedEvents = mutableListOf<HapticEvent>()
        var isPlaying = false

        override fun isHapticSupported(): Boolean = supported
        override fun loadPattern(pattern: HapticPattern) {}
        override fun start() { isPlaying = true }
        override fun stop() { isPlaying = false }
        override fun release() { stop() }
        override fun playEvent(event: HapticEvent) {
            playedEvents.add(event)
        }
    }

    private class FakeVideoRepo : VideoRepository {
        val video = VideoItem(
            id = "vid_sync_01",
            title = "Test Video",
            description = "Test Description",
            durationMs = 20_000L,
            videoResUri = "sample",
            hapticConfigResName = "haptics"
        )
        override fun getVideos(): Flow<List<VideoItem>> = flowOf(listOf(video))
        override suspend fun getVideoById(id: String): VideoItem = video
    }

    private class FakeHapticRepo(val pattern: HapticPattern) : HapticRepository {
        override suspend fun loadHapticPatternForVideo(videoId: String): Result<HapticPattern> {
            return Result.success(pattern)
        }
    }

    @Test
    fun timelineUpdate_dispatchesToSynchronizerAndTelemetry() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val fakeEngine = FakeHapticEngine(supported = true)
        val synchronizer = HapticSynchronizer(engine = fakeEngine, toleranceMs = 50L)
        val event = HapticEvent(
            startTimeMs = 2000L,
            durationMs = 100L,
            intensity = 0.8f,
            parameters = mapOf("frequencyHz" to 210.0f, "amplitude" to 0.8f, "description" to "Pacinian pulse")
        )
        val pattern = HapticPattern(videoId = "vid_sync_01", events = listOf(event))

        val viewModel = VideoPlayerViewModel(
            videoRepository = FakeVideoRepo(),
            hapticRepository = FakeHapticRepo(pattern),
            hapticSynchronizer = synchronizer,
            hapticEngine = fakeEngine,
            coroutineScope = testScope
        )

        viewModel.loadVideo("vid_sync_01", sessionHapticsEnabled = true)

        assertEquals(HapticCapabilityLevel.SUPPORTED, viewModel.uiState.value.capabilityLevel)
        assertTrue(viewModel.uiState.value.isHapticSupported)
        assertTrue(viewModel.uiState.value.isHapticsEnabled)

        // Before event
        viewModel.onTimelinePositionChanged(1000L)
        assertEquals(0, fakeEngine.playedEvents.size)
        assertNull(viewModel.uiState.value.currentEventFrequencyHz)

        // At event
        viewModel.onTimelinePositionChanged(2010L)
        assertEquals(1, fakeEngine.playedEvents.size)
        assertEquals(2000L, fakeEngine.playedEvents[0].startTimeMs)

        // Telemetry updated
        assertEquals(210.0f, viewModel.uiState.value.currentEventFrequencyHz)
        assertEquals(0.8f, viewModel.uiState.value.currentEventAmplitude)
        assertEquals(100L, viewModel.uiState.value.currentEventDurationMs)
        assertEquals("Pacinian pulse", viewModel.uiState.value.currentEventDescription)
    }

    @Test
    fun seekTo_updatesSynchronizerAndPosition() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val fakeEngine = FakeHapticEngine(supported = true)
        val synchronizer = HapticSynchronizer(engine = fakeEngine)
        val viewModel = VideoPlayerViewModel(
            videoRepository = FakeVideoRepo(),
            hapticRepository = FakeHapticRepo(HapticPattern(videoId = "vid_sync_01")),
            hapticSynchronizer = synchronizer,
            hapticEngine = fakeEngine,
            coroutineScope = testScope
        )

        viewModel.loadVideo("vid_sync_01")

        viewModel.seekTo(5000L)
        assertEquals(5000L, viewModel.uiState.value.currentPositionMs)
        assertFalse(viewModel.uiState.value.isCompleted)

        // Seek past end clamps to duration
        viewModel.seekTo(25000L)
        assertEquals(20000L, viewModel.uiState.value.currentPositionMs)
        assertTrue(viewModel.uiState.value.isCompleted)
    }

    @Test
    fun debugMode_toggleable() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = VideoPlayerViewModel(coroutineScope = testScope)
        assertFalse(viewModel.uiState.value.isDebugMode)

        viewModel.toggleDebugMode()
        assertTrue(viewModel.uiState.value.isDebugMode)

        viewModel.toggleDebugMode()
        assertFalse(viewModel.uiState.value.isDebugMode)
    }

    @Test
    fun unsupportedHardware_handlesGracefully() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val unsupportedEngine = FakeHapticEngine(supported = false)
        val synchronizer = HapticSynchronizer(engine = unsupportedEngine)
        val viewModel = VideoPlayerViewModel(
            videoRepository = FakeVideoRepo(),
            hapticSynchronizer = synchronizer,
            hapticEngine = unsupportedEngine,
            coroutineScope = testScope
        )

        viewModel.loadVideo("vid_sync_01")

        assertFalse(viewModel.uiState.value.isHapticSupported)
        assertFalse(viewModel.uiState.value.isHapticsEnabled)
        assertEquals(HapticCapabilityLevel.UNAVAILABLE, viewModel.uiState.value.capabilityLevel)

        // Toggle haptics cannot re-enable unsupported hardware
        viewModel.toggleHaptics()
        assertFalse(viewModel.uiState.value.isHapticsEnabled)
    }

    @Test
    fun playbackPosition_persistsAcrossConfigurationRestoration() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = VideoPlayerViewModel(coroutineScope = testScope)
        viewModel.loadVideo("vid_sync_01")

        viewModel.savePlaybackPosition(12500L)
        assertEquals(12500L, viewModel.getSavedPlaybackPosition())
    }

    @Test
    fun configurationChange_doesNotDuplicateHapticEvents() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val fakeEngine = FakeHapticEngine(supported = true)
        val synchronizer = HapticSynchronizer(engine = fakeEngine, toleranceMs = 50L)
        val event = HapticEvent(
            startTimeMs = 3000L,
            durationMs = 200L,
            intensity = 0.8f,
            parameters = mapOf("frequencyHz" to 180.0f)
        )
        val pattern = HapticPattern(videoId = "vid_sync_01", events = listOf(event))

        val viewModel = VideoPlayerViewModel(
            videoRepository = FakeVideoRepo(),
            hapticRepository = FakeHapticRepo(pattern),
            hapticSynchronizer = synchronizer,
            hapticEngine = fakeEngine,
            coroutineScope = testScope
        )

        viewModel.loadVideo("vid_sync_01")

        // First pass through event
        viewModel.onTimelinePositionChanged(3010L)
        assertEquals(1, fakeEngine.playedEvents.size)

        // Rotation occurs: saved position restored, timeline ticks at same or subsequent position
        viewModel.savePlaybackPosition(3010L)
        val restoredPos = viewModel.getSavedPlaybackPosition()
        viewModel.onTimelinePositionChanged(restoredPos)
        viewModel.onTimelinePositionChanged(3050L)

        // Must still be 1 (no duplicate actuation)
        assertEquals(1, fakeEngine.playedEvents.size)
    }

    @Test
    fun backwardSeek_restoresEventEligibility() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val fakeEngine = FakeHapticEngine(supported = true)
        val synchronizer = HapticSynchronizer(engine = fakeEngine, toleranceMs = 50L)
        val event = HapticEvent(
            startTimeMs = 4000L,
            durationMs = 200L,
            intensity = 0.8f,
            parameters = mapOf("frequencyHz" to 180.0f)
        )
        val pattern = HapticPattern(videoId = "vid_sync_01", events = listOf(event))

        val viewModel = VideoPlayerViewModel(
            videoRepository = FakeVideoRepo(),
            hapticRepository = FakeHapticRepo(pattern),
            hapticSynchronizer = synchronizer,
            hapticEngine = fakeEngine,
            coroutineScope = testScope
        )

        viewModel.loadVideo("vid_sync_01")

        // First play through
        viewModel.onTimelinePositionChanged(4020L)
        assertEquals(1, fakeEngine.playedEvents.size)

        // Seek backwards to 1000L
        viewModel.seekTo(1000L)
        assertFalse(fakeEngine.isPlaying)

        // Play forward again across event
        viewModel.onTimelinePositionChanged(4010L)
        assertEquals(2, fakeEngine.playedEvents.size)
    }
}
