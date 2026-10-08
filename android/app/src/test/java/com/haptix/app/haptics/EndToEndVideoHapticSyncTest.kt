package com.haptix.app.haptics

import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticFrequencyPattern
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.model.VideoItem
import com.haptix.app.data.parser.FrequencyPatternParser
import com.haptix.app.data.repository.HapticRepository
import com.haptix.app.data.repository.VideoRepository
import com.haptix.app.ui.screens.video.VideoPlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * End-to-End verification suite for Video + Haptic Synchronization demo.
 *
 * Validates:
 * 1. Sample haptic JSON parsing and constraint validation.
 * 2. Chronological event ordering.
 * 3. Timeline progression and event dispatching.
 * 4. Seek forward (skipping past events).
 * 5. Seek backward (restoring event eligibility).
 * 6. Pause and resume behavior.
 * 7. Deduplication of haptic events across rapid ticks.
 * 8. Video completion cleanup.
 * 9. Haptics disabled behavior.
 * 10. ViewModel + Synchronizer + FrequencyPattern + HapticEngine integration.
 */
class EndToEndVideoHapticSyncTest {

    private class RecordingHapticEngine : HapticEngine {
        val playedEvents = mutableListOf<HapticEvent>()
        var startCount = 0
        var stopCount = 0
        var isPlaying = false

        override fun isHapticSupported(): Boolean = true

        override fun loadPattern(pattern: HapticPattern) {}

        override fun start() {
            startCount++
            isPlaying = true
        }

        override fun stop() {
            stopCount++
            isPlaying = false
        }

        override fun release() {
            stop()
        }

        override fun playEvent(event: HapticEvent) {
            playedEvents.add(event)
        }
    }

    private lateinit var engine: RecordingHapticEngine
    private lateinit var synchronizer: HapticSynchronizer
    private lateinit var sampleFrequencyPattern: HapticFrequencyPattern

    private val sampleJson: String = """
        {
          "format": "HaptiX-Frequency-Stimulus",
          "version": "1.0",
          "source": "AOSP Haptics Architecture Guidelines & Pacinian Corpuscle Psychophysics",
          "sourceUrl": "https://source.android.com/devices/sensors/haptics",
          "description": "DEMO / EXPERIMENTAL DATA: Low-frequency dynamics evaluation",
          "frequencyUnit": "Hz",
          "timingUnit": "ms",
          "videoId": "video_01",
          "points": [
            { "startTimeMs": 1500, "durationMs": 250, "frequencyHz": 150.0, "amplitude": 0.65, "description": "Tactile test pulse (150 Hz)", "provenance": "perception-literature motivated" },
            { "startTimeMs": 5000, "durationMs": 300, "frequencyHz": 160.0, "amplitude": 0.70, "description": "Lower-band test sweep tone (160 Hz)", "provenance": "experimentally selected by HaptiX" },
            { "startTimeMs": 9500, "durationMs": 150, "frequencyHz": 175.0, "amplitude": 0.80, "description": "Experimental lower-band transient (175 Hz)", "provenance": "experimentally selected by HaptiX" },
            { "startTimeMs": 14000, "durationMs": 400, "frequencyHz": 150.0, "amplitude": 0.60, "description": "Sustained texture pulse (150 Hz)", "provenance": "perception-literature motivated" },
            { "startTimeMs": 20000, "durationMs": 200, "frequencyHz": 180.0, "amplitude": 0.85, "description": "Mid-low experimental transition pulse (180 Hz)", "provenance": "experimentally selected by HaptiX" },
            { "startTimeMs": 26000, "durationMs": 350, "frequencyHz": 165.0, "amplitude": 0.75, "description": "Terminal lower-band release (165 Hz)", "provenance": "experimentally selected by HaptiX" }
          ]
        }
    """.trimIndent()

    @Before
    fun setUp() {
        engine = RecordingHapticEngine()
        synchronizer = HapticSynchronizer(engine = engine, toleranceMs = 50L)

        val parseResult = FrequencyPatternParser.parseJson(sampleJson)
        assertTrue("Parsing sample haptic data failed: ${parseResult.exceptionOrNull()?.message}", parseResult.isSuccess)
        sampleFrequencyPattern = parseResult.getOrThrow()
        synchronizer.setFrequencyPattern(sampleFrequencyPattern)
    }

    @Test
    fun parseSampleHapticData_validatesPhysicalConstraintsAndMetadata() {
        assertEquals("video_01", sampleFrequencyPattern.videoId)
        assertEquals(6, sampleFrequencyPattern.pointCount)
        assertTrue(sampleFrequencyPattern.description.contains("DEMO"))

        for (point in sampleFrequencyPattern.points) {
            assertTrue("startTimeMs must be >= 0", point.startTimeMs >= 0L)
            assertTrue("durationMs must be > 0", point.durationMs > 0L)
            assertTrue("frequencyHz must be in physical range (100..300Hz)", point.frequencyHz in 100f..300f)
            assertTrue("amplitude must be in [0..1]", point.amplitude in 0f..1f)
            assertNotNull(point.provenance)
        }
    }

    @Test
    fun sampleHapticData_enforcesChronologicalOrdering() {
        val points = sampleFrequencyPattern.points
        for (i in 0 until points.size - 1) {
            assertTrue(
                "Point at $i (${points[i].startTimeMs}ms) must precede point at ${i + 1} (${points[i + 1].startTimeMs}ms)",
                points[i].startTimeMs <= points[i + 1].startTimeMs
            )
        }
        assertEquals(1500L, points[0].startTimeMs)
        assertEquals(5000L, points[1].startTimeMs)
        assertEquals(9500L, points[2].startTimeMs)
        assertEquals(14000L, points[3].startTimeMs)
        assertEquals(20000L, points[4].startTimeMs)
        assertEquals(26000L, points[5].startTimeMs)
    }

    @Test
    fun timelineForwardPlayback_dispatchesEventsWithinToleranceWindow() {
        // Step before first event
        synchronizer.onTimelineUpdate(500L)
        assertEquals(0, engine.playedEvents.size)

        // Step entering first event activation window [1500..1550]
        synchronizer.onTimelineUpdate(1520L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals(1500L, engine.playedEvents[0].startTimeMs)
        val freq1 = engine.playedEvents[0].parameters["frequencyHz"] as Float
        assertEquals(150.0f, freq1)

        // Multiple subsequent updates in the same window do not duplicate
        synchronizer.onTimelineUpdate(1530L)
        synchronizer.onTimelineUpdate(1540L)
        assertEquals(1, engine.playedEvents.size)

        // Advance to second event (5000ms)
        synchronizer.onTimelineUpdate(4950L)
        assertEquals(1, engine.playedEvents.size)
        synchronizer.onTimelineUpdate(5020L)
        assertEquals(2, engine.playedEvents.size)
        assertEquals(5000L, engine.playedEvents[1].startTimeMs)
        val freq2 = engine.playedEvents[1].parameters["frequencyHz"] as Float
        assertEquals(160.0f, freq2)
    }

    @Test
    fun pauseAndResumeBehavior_stopsAndResumesHapticEngine() {
        val initialStops = engine.stopCount
        val initialStarts = engine.startCount

        // Pause
        synchronizer.onPause()
        assertEquals(initialStops + 1, engine.stopCount)
        assertFalse(engine.isPlaying)

        // Resume at 3000ms
        synchronizer.onResume(3000L)
        assertEquals(initialStarts + 1, engine.startCount)
        assertTrue(engine.isPlaying)
    }

    @Test
    fun seekForwardBehavior_skipsAlreadyPlayedAndSkippedEvents() {
        // Seek forward to 8000ms (skipping 1500ms and 5000ms events)
        synchronizer.onSeek(8000L)
        assertEquals(0, engine.playedEvents.size)

        // Progress past 9500ms event
        synchronizer.onTimelineUpdate(9520L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals(9500L, engine.playedEvents[0].startTimeMs)

        // 1500ms and 5000ms events must never be played
        for (event in engine.playedEvents) {
            assertTrue(event.startTimeMs >= 8000L)
        }
    }

    @Test
    fun seekBackwardBehavior_restoresEventEligibility() {
        // Play first two events
        synchronizer.onTimelineUpdate(1520L)
        synchronizer.onTimelineUpdate(5020L)
        assertEquals(2, engine.playedEvents.size)

        // Seek backward to 3000ms (between event 1 and event 2)
        synchronizer.onSeek(3000L)
        engine.playedEvents.clear()

        // Progress past event 2 (5000ms) again
        synchronizer.onTimelineUpdate(5010L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals(5000L, engine.playedEvents[0].startTimeMs)

        // Event 1 (1500ms) preceded seek target 3000ms, so it must NOT replay
        for (event in engine.playedEvents) {
            assertTrue(event.startTimeMs >= 3000L)
        }
    }

    @Test
    fun skippingAlreadyPlayedEvents_noDuplicatesOnRepeatedTicks() {
        // Rapid ticks simulating 60fps / 16ms poll frames around 1500ms
        for (t in 1480L..1560L step 10L) {
            synchronizer.onTimelineUpdate(t)
        }
        assertEquals(1, engine.playedEvents.size)
        assertEquals(1500L, engine.playedEvents[0].startTimeMs)
    }

    @Test
    fun videoEndCleanup_stopsEngineAndFinishesSession() {
        val stopsBefore = engine.stopCount
        synchronizer.onPlaybackComplete()
        assertEquals(stopsBefore + 1, engine.stopCount)
        assertFalse(engine.isPlaying)
    }

    @Test
    fun hapticsDisabledBehavior_suppressesAllActuation() {
        synchronizer.isHapticsEnabled = false

        // Timeline advances through all event timestamps
        for (t in 0L..30000L step 1000L) {
            synchronizer.onTimelineUpdate(t)
        }
        assertEquals(0, engine.playedEvents.size)

        // Re-enabling allows subsequent events
        synchronizer.onSeek(25000L)
        synchronizer.isHapticsEnabled = true
        synchronizer.onTimelineUpdate(26020L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals(26000L, engine.playedEvents[0].startTimeMs)
    }

    @Test
    fun e2eViewModelExoPlayerSimulation_integratesAllComponents() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)

        val mockVideoRepo = object : VideoRepository {
            override fun getVideos(): Flow<List<VideoItem>> = flowOf(emptyList())
            override suspend fun getVideoById(id: String): VideoItem = VideoItem(
                id = "video_01",
                title = "Test Clip",
                durationMs = 30_000L,
                videoResUri = "sample_video_01"
            )
        }

        val mockHapticRepo = object : HapticRepository {
            override suspend fun loadFrequencyPatternForVideo(videoId: String): Result<HapticFrequencyPattern> {
                return Result.success(sampleFrequencyPattern)
            }
            override suspend fun loadHapticPatternForVideo(videoId: String): Result<HapticPattern> {
                return Result.success(sampleFrequencyPattern.toHapticPattern())
            }
        }

        val viewModel = VideoPlayerViewModel(
            videoRepository = mockVideoRepo,
            hapticRepository = mockHapticRepo,
            hapticSynchronizer = synchronizer,
            hapticEngine = engine,
            coroutineScope = testScope
        )

        viewModel.loadVideo("video_01")

        assertEquals("video_01", viewModel.uiState.value.videoItem?.id)
        assertTrue(viewModel.uiState.value.isHapticsEnabled)

        // 1. Play state transition from ExoPlayer
        viewModel.updatePlaybackState(isPlaying = true, isCompleted = false)
        assertTrue(engine.isPlaying)

        // 2. Timeline updates from ExoPlayer
        viewModel.onTimelinePositionChanged(1520L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals(150.0f, viewModel.uiState.value.currentEventFrequencyHz)

        // 3. Pause state transition from ExoPlayer
        viewModel.updatePlaybackState(isPlaying = false, isCompleted = false)
        assertFalse(engine.isPlaying)

        // 4. Seek to 9000ms
        viewModel.seekTo(9000L)
        assertEquals(9000L, viewModel.uiState.value.currentPositionMs)

        // 5. Resume and step past 9500ms
        viewModel.updatePlaybackState(isPlaying = true, isCompleted = false)
        viewModel.onTimelinePositionChanged(9520L)
        assertEquals(2, engine.playedEvents.size)
        assertEquals(9500L, engine.playedEvents[1].startTimeMs)

        // 6. Disable haptics via UI toggle
        viewModel.setHapticsEnabled(false)
        assertFalse(viewModel.uiState.value.isHapticsEnabled)
        assertFalse(synchronizer.isHapticsEnabled)

        // 7. Step past 14000ms (should not trigger)
        viewModel.onTimelinePositionChanged(14020L)
        assertEquals(2, engine.playedEvents.size)

        // 8. Video completion
        viewModel.updatePlaybackState(isPlaying = false, isCompleted = true)
        assertTrue(viewModel.uiState.value.isCompleted)
        assertFalse(engine.isPlaying)
    }
}
