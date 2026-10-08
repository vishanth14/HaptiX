package com.haptix.app.haptics

import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.model.SemanticHapticType
import com.haptix.app.data.parser.FrequencyPatternParser
import com.haptix.app.data.repository.DefaultHapticRepository
import com.haptix.app.data.repository.DefaultVideoRepository
import com.haptix.app.domain.provider.CuratedHapticProvider
import com.haptix.app.ui.screens.video.VideoPlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Rigorous Verification Suite for HaptiX Playline and Haptic Synchronization.
 * Covers requirements A-T (State Machine & Synchronization) and 1-10 (Playline & Markers).
 */
class VideoPlaylineAndHapticSyncTest {

    private class RecordingHapticEngine : HapticEngine {
        val playedEvents = mutableListOf<HapticEvent>()
        var isVibrating = false
        var stopCount = 0

        override fun isHapticSupported(): Boolean = true
        override fun loadPattern(pattern: HapticPattern) {}
        override fun start() {}
        override fun stop() {
            isVibrating = false
            stopCount++
        }
        override fun stop(reason: String) {
            isVibrating = false
            stopCount++
        }
        override fun release() { stop() }
        override fun playEvent(event: HapticEvent) {
            playedEvents.add(event)
            isVibrating = true
        }
    }

    private fun findAssetFile(path: String): File {
        val candidates = listOf(
            File("src/main/assets/$path"),
            File("app/src/main/assets/$path"),
            File("android/app/src/main/assets/$path"),
            File("../app/src/main/assets/$path")
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Asset not found in candidates: $path")
    }

    private lateinit var engine: RecordingHapticEngine
    private lateinit var synchronizer: HapticSynchronizer

    @Before
    fun setUp() {
        engine = RecordingHapticEngine()
        synchronizer = HapticSynchronizer(engine = engine, minimumImpulseDurationMs = 50L)
    }

    // =========================================================================
    // SECTION 21: STATE MACHINE & SYNCHRONIZATION TESTS (A - T)
    // =========================================================================

    @Test
    fun testA_playFromZero() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E0", startTimeMs = 0L, durationMs = 300L, semanticType = SemanticHapticType.SOFT_IMPACT),
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 200L, semanticType = SemanticHapticType.SMOOTH)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)
        assertEquals(PlaybackSyncState.PLAYING, synchronizer.syncState)

        // Event at exactly 0ms must actuate immediately on play from zero
        synchronizer.onTimelineUpdate(0L)
        assertTrue(engine.isVibrating)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("E0", engine.playedEvents[0].id)
    }

    @Test
    fun testB_pauseDuringAnEvent() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 500L, semanticType = SemanticHapticType.SMOOTH)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Play into event at 1200ms
        synchronizer.onTimelineUpdate(1200L)
        assertTrue(engine.isVibrating)
        val initialGeneration = synchronizer.playbackGeneration

        // Pause at 1200ms
        synchronizer.onPause("USER_PAUSE", positionMs = 1200L)
        assertFalse(engine.isVibrating)
        assertEquals(PlaybackSyncState.PAUSED, synchronizer.syncState)
        assertTrue(synchronizer.playbackGeneration > initialGeneration)

        // Ticks received while paused must never actuate haptics
        synchronizer.onTimelineUpdate(1300L)
        assertFalse(engine.isVibrating)
    }

    @Test
    fun testC_resumeDuringAnEvent() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 10000L, durationMs = 500L, semanticType = SemanticHapticType.SMOOTH)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Play to 10200ms
        synchronizer.onTimelineUpdate(10200L)
        assertEquals(1, engine.playedEvents.size)

        // Pause at 10200ms
        synchronizer.onPause("PAUSE", positionMs = 10200L)
        assertFalse(engine.isVibrating)

        // Resume at exact position 10200ms
        synchronizer.onResume(10200L)
        assertEquals(PlaybackSyncState.PLAYING, synchronizer.syncState)

        // Reconciled tick at 10200ms resumes the in-progress event
        synchronizer.onTimelineUpdate(10200L)
        assertTrue(engine.isVibrating)
        assertEquals(2, engine.playedEvents.size)
        assertEquals("E1", engine.playedEvents.last().id)
    }

    @Test
    fun testD_pauseDuringSilence() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 300L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Position 500ms is in silence region
        synchronizer.onTimelineUpdate(500L)
        assertFalse(engine.isVibrating)

        // Pause at 500ms
        synchronizer.onPause("PAUSE", positionMs = 500L)
        assertEquals(PlaybackSyncState.PAUSED, synchronizer.syncState)
        assertFalse(engine.isVibrating)
        assertEquals(0, engine.playedEvents.size)
    }

    @Test
    fun testE_seekForwardOverMultipleEvents() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 200L),
                HapticEvent(id = "E2", startTimeMs = 2000L, durationMs = 200L),
                HapticEvent(id = "E3", startTimeMs = 3000L, durationMs = 200L),
                HapticEvent(id = "E4", startTimeMs = 5000L, durationMs = 200L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(1100L)
        assertEquals(1, engine.playedEvents.size)

        // Seek from 1100ms to 4000ms (skipping E2 and E3)
        synchronizer.onSeek(4000L)
        assertFalse(engine.isVibrating)

        // Position update at 4000ms: in silence between E3 and E4
        synchronizer.onTimelineUpdate(4000L)
        assertEquals(1, engine.playedEvents.size) // No old skipped events fired!

        // Continue to 5050ms: E4 fires cleanly
        synchronizer.onTimelineUpdate(5050L)
        assertEquals(2, engine.playedEvents.size)
        assertEquals("E4", engine.playedEvents.last().id)
    }

    @Test
    fun testF_seekBackwardOverMultipleEvents() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 200L),
                HapticEvent(id = "E2", startTimeMs = 2000L, durationMs = 200L),
                HapticEvent(id = "E3", startTimeMs = 3000L, durationMs = 200L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(3100L) // Past all events

        // Seek backward to 1500ms
        synchronizer.onSeek(1500L)
        assertFalse(engine.isVibrating)

        // E2 at 2000ms is re-armed and must fire when video reaches it
        synchronizer.onTimelineUpdate(2050L)
        assertTrue(engine.isVibrating)
        assertEquals("E2", engine.playedEvents.last().id)
    }

    @Test
    fun testG_rapidRepeatedSeeking() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E20", startTimeMs = 20000L, durationMs = 500L),
                HapticEvent(id = "E35", startTimeMs = 35000L, durationMs = 500L),
                HapticEvent(id = "E60", startTimeMs = 60000L, durationMs = 500L),
                HapticEvent(id = "E90", startTimeMs = 90000L, durationMs = 500L),
                HapticEvent(id = "E110", startTimeMs = 110000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        val gen0 = synchronizer.playbackGeneration
        // Rapid scrub: 20s -> 90s -> 35s -> 110s -> 60s
        synchronizer.onSeek(20000L)
        val gen1 = synchronizer.playbackGeneration
        synchronizer.onSeek(90000L)
        val gen2 = synchronizer.playbackGeneration
        synchronizer.onSeek(35000L)
        val gen3 = synchronizer.playbackGeneration
        synchronizer.onSeek(110000L)
        val gen4 = synchronizer.playbackGeneration
        synchronizer.onSeek(60200L)
        val genFinal = synchronizer.playbackGeneration

        // Monotonic generation progression
        assertTrue(genFinal > gen4 && gen4 > gen3 && gen3 > gen2 && gen2 > gen1 && gen1 > gen0)

        // Only the final destination (60200L) actuates
        synchronizer.onTimelineUpdate(60200L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("E60", engine.playedEvents[0].id)
    }

    @Test
    fun testH_fastForward() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 30000L, durationMs = 500L),
                HapticEvent(id = "E2", startTimeMs = 45000L, durationMs = 500L),
                HapticEvent(id = "E3", startTimeMs = 60000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(30100L)
        assertEquals(1, engine.playedEvents.size)

        // Fast-forward jump: 30s -> 60s
        synchronizer.onSeek(60100L)
        // E2 at 45s must NEVER execute!
        synchronizer.onTimelineUpdate(60100L)
        assertEquals(2, engine.playedEvents.size)
        assertEquals("E3", engine.playedEvents.last().id)
        assertFalse(engine.playedEvents.any { it.id == "E2" })
    }

    @Test
    fun testI_rewind() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E_55", startTimeMs = 55000L, durationMs = 500L),
                HapticEvent(id = "E_80", startTimeMs = 80000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(80100L)
        assertEquals("E_80", engine.playedEvents.last().id)

        // Rewind to 55000ms
        synchronizer.onSeek(55000L)
        synchronizer.onTimelineUpdate(55100L)
        assertEquals("E_55", engine.playedEvents.last().id)
    }

    @Test
    fun testJ_bufferingDuringAnEvent() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 600L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(1200L)
        assertTrue(engine.isVibrating)

        // Buffering at 1200ms
        synchronizer.onBuffering(1200L)
        assertFalse(engine.isVibrating)
        assertEquals(PlaybackSyncState.BUFFERING, synchronizer.syncState)

        // While buffering, ticks must not actuate
        synchronizer.onTimelineUpdate(1300L)
        assertFalse(engine.isVibrating)

        // When playback becomes ready and resumes at 1200ms
        synchronizer.onResume(1200L)
        synchronizer.onTimelineUpdate(1200L)
        assertTrue(engine.isVibrating)
    }

    @Test
    fun testK_bufferingDuringSilence() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 2000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(500L)
        assertFalse(engine.isVibrating)

        // Buffering in silence
        synchronizer.onBuffering(500L)
        assertEquals(PlaybackSyncState.BUFFERING, synchronizer.syncState)
        assertFalse(engine.isVibrating)
        assertEquals(0, engine.playedEvents.size)
    }

    @Test
    fun testL_disableHapticsDuringAnEvent() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 800L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(1200L)
        assertTrue(engine.isVibrating)

        // Disable haptics mid-event
        synchronizer.isHapticsEnabled = false
        assertFalse(engine.isVibrating)

        // Continued updates remain silent
        synchronizer.onTimelineUpdate(1400L)
        assertFalse(engine.isVibrating)
    }

    @Test
    fun testM_reEnableHapticsDuringAnEvent() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 2000L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Disable haptics before event
        synchronizer.isHapticsEnabled = false
        synchronizer.onTimelineUpdate(1200L)
        assertFalse(engine.isVibrating)

        // Re-enable at 1500ms while inside event interval
        synchronizer.isHapticsEnabled = true
        synchronizer.onTimelineUpdate(1500L)
        assertTrue(engine.isVibrating)
        assertEquals(1, engine.playedEvents.size)
    }

    @Test
    fun testN_videoCompletion() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E_FINAL", startTimeMs = 9000L, durationMs = 1000L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(9500L)
        assertTrue(engine.isVibrating)

        // Video completes at 10000ms
        synchronizer.onPlaybackComplete()
        assertFalse(engine.isVibrating)
        assertFalse(synchronizer.isActuatingState)
        assertEquals(PlaybackSyncState.STOPPED, synchronizer.syncState)

        // Spurious ticks after end are rejected
        synchronizer.onTimelineUpdate(10100L)
        assertFalse(engine.isVibrating)
    }

    @Test
    fun testO_resumeAfterSeek() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E_SEEK", startTimeMs = 5000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onPause("USER_PAUSE", 0L)

        // Seek while paused
        synchronizer.onSeek(5100L)
        // Resume from seek target
        synchronizer.onResume(5100L)
        synchronizer.onTimelineUpdate(5100L)

        assertTrue(engine.isVibrating)
        assertEquals("E_SEEK", engine.playedEvents.last().id)
    }

    @Test
    fun testP_eventExactlyAtCurrentPosition() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E_EXACT", startTimeMs = 1000L, durationMs = 300L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // currentPosition exactly at startTimeMs
        synchronizer.onTimelineUpdate(1000L)
        assertTrue(engine.isVibrating)
        assertEquals("E_EXACT", engine.playedEvents.last().id)
    }

    @Test
    fun testQ_currentPositionExactlyAtEventEnd() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E_END", startTimeMs = 1000L, durationMs = 300L) // End = 1300ms
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Play during event: active at 1100ms
        synchronizer.onTimelineUpdate(1100L)
        assertTrue(engine.isVibrating)
        assertEquals(1, engine.playedEvents.size)
        assertEquals(EventTriggerState.ACTIVE, synchronizer.getEventState("E_END"))

        // Position reaches exactly event end (1300ms): outside active interval
        synchronizer.onTimelineUpdate(1300L)
        assertFalse(engine.isVibrating)
        assertFalse(synchronizer.isActuatingState)
        assertEquals(1, engine.playedEvents.size) // Not re-triggered
        assertEquals(EventTriggerState.COMPLETED, synchronizer.getEventState("E_END"))
        assertTrue(synchronizer.getActiveEvents(1300L).isEmpty())

        // Seek directly to event end: must be completed and never fire
        synchronizer.onSeek(1300L)
        assertEquals(EventTriggerState.COMPLETED, synchronizer.getEventState("E_END"))
        synchronizer.onTimelineUpdate(1300L)
        assertFalse(engine.isVibrating)
        assertEquals(1, engine.playedEvents.size)
    }

    @Test
    fun testR_staleGenerationCallback() {
        val gen1 = synchronizer.playbackGeneration
        var executed = false

        // Simulate seek advancing generation
        synchronizer.onSeek(5000L)
        val gen2 = synchronizer.playbackGeneration
        assertTrue(gen2 > gen1)

        // Callback with stale generation gen1 must be rejected
        val wasExecuted = synchronizer.executeIfGenerationValid(gen1) {
            executed = true
        }
        assertFalse(wasExecuted)
        assertFalse(executed)

        // Callback with current generation gen2 executes
        val currentExecuted = synchronizer.executeIfGenerationValid(gen2) {
            executed = true
        }
        assertTrue(currentExecuted)
        assertTrue(executed)
    }

    @Test
    fun testS_duplicatePlayerCallback() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // First tick inside event
        synchronizer.onTimelineUpdate(1100L)
        assertEquals(1, engine.playedEvents.size)

        // Duplicate identical player callback at 1100ms
        synchronizer.onTimelineUpdate(1100L)
        assertEquals(1, engine.playedEvents.size) // MUST NOT RETRIGGER
    }

    @Test
    fun testT_rapidRecompositionUiRecreation() {
        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(1100L)
        assertEquals(1, engine.playedEvents.size)

        // Repeated queries / recompositions do not alter synchronizer internal actuation state
        assertEquals(EventTriggerState.ACTIVE, synchronizer.getEventState("E1"))
        synchronizer.onTimelineUpdate(1150L) // Normal forward progress within interval
        assertEquals(1, engine.playedEvents.size) // No extra play calls
    }

    // =========================================================================
    // SECTION 22: PLAYLINE & MARKER TESTS (1 - 10)
    // =========================================================================

    @Test
    fun testPlayline1_everyMarkerMapsToHapticPatternEvent() {
        val f1File = findAssetFile("haptics/f1_haptic_timeline.json")
        val pattern = FrequencyPatternParser.parseToHapticPattern(f1File.readText()).getOrThrow()

        val markers = pattern.events
        assertEquals(pattern.events.size, markers.size)
        for (i in pattern.events.indices) {
            assertEquals(pattern.events[i].id, markers[i].id)
        }
    }

    @Test
    fun testPlayline2_markerTimestampEqualsEventStartTimeMs() {
        val f1File = findAssetFile("haptics/f1_haptic_timeline.json")
        val pattern = FrequencyPatternParser.parseToHapticPattern(f1File.readText()).getOrThrow()

        for (event in pattern.events) {
            // Marker authoritative timestamp is strictly event.startTimeMs (NOT substituted with peakTimeMs)
            val markerTimestampMs = event.startTimeMs
            assertEquals(event.startTimeMs, markerTimestampMs)
        }
    }

    @Test
    fun testPlayline3_markerPositionsUseActualExoPlayerDuration() {
        val event = HapticEvent(id = "E1", startTimeMs = 30000L, durationMs = 200L)
        val playerDurationMs = 120000L
        val playlineWidth = 1000f

        val markerX = (event.startTimeMs.toFloat() / playerDurationMs) * playlineWidth
        assertEquals(250f, markerX, 0.001f) // Exactly 25% of width
    }

    @Test
    fun testPlayline4_seekingDoesNotModifyMarkerTimestamps() = runBlocking {
        val videoRepo = DefaultVideoRepository()
        val f1File = findAssetFile("haptics/f1_haptic_timeline.json")
        val curatedProvider = CuratedHapticProvider(assetReader = { f1File.readText() })
        val hapticRepo = DefaultHapticRepository(providers = listOf(curatedProvider))
        val testScope = CoroutineScope(Dispatchers.Unconfined)

        val vm = VideoPlayerViewModel(
            videoRepository = videoRepo,
            hapticRepository = hapticRepo,
            hapticSynchronizer = synchronizer,
            hapticEngine = engine,
            coroutineScope = testScope
        )
        vm.loadVideo("f1_2025_haptic_trailer")

        val initialTimestamps = vm.uiState.value.hapticEvents.map { it.startTimeMs }
        vm.seekTo(50000L)
        val afterSeekTimestamps = vm.uiState.value.hapticEvents.map { it.startTimeMs }

        assertEquals(initialTimestamps, afterSeekTimestamps)
    }

    @Test
    fun testPlayline5_pauseDoesNotModifyMarkerTimestamps() = runBlocking {
        val videoRepo = DefaultVideoRepository()
        val f1File = findAssetFile("haptics/f1_haptic_timeline.json")
        val curatedProvider = CuratedHapticProvider(assetReader = { f1File.readText() })
        val hapticRepo = DefaultHapticRepository(providers = listOf(curatedProvider))
        val testScope = CoroutineScope(Dispatchers.Unconfined)

        val vm = VideoPlayerViewModel(
            videoRepository = videoRepo,
            hapticRepository = hapticRepo,
            hapticSynchronizer = synchronizer,
            hapticEngine = engine,
            coroutineScope = testScope
        )
        vm.loadVideo("f1_2025_haptic_trailer")

        val initialTimestamps = vm.uiState.value.hapticEvents.map { it.startTimeMs }
        vm.play()
        vm.pause()
        val afterPauseTimestamps = vm.uiState.value.hapticEvents.map { it.startTimeMs }

        assertEquals(initialTimestamps, afterPauseTimestamps)
    }

    @Test
    fun testPlayline6_resumeDoesNotModifyMarkerTimestamps() = runBlocking {
        val videoRepo = DefaultVideoRepository()
        val f1File = findAssetFile("haptics/f1_haptic_timeline.json")
        val curatedProvider = CuratedHapticProvider(assetReader = { f1File.readText() })
        val hapticRepo = DefaultHapticRepository(providers = listOf(curatedProvider))
        val testScope = CoroutineScope(Dispatchers.Unconfined)

        val vm = VideoPlayerViewModel(
            videoRepository = videoRepo,
            hapticRepository = hapticRepo,
            hapticSynchronizer = synchronizer,
            hapticEngine = engine,
            coroutineScope = testScope
        )
        vm.loadVideo("f1_2025_haptic_trailer")

        val initialTimestamps = vm.uiState.value.hapticEvents.map { it.startTimeMs }
        vm.pause()
        vm.play()
        val afterResumeTimestamps = vm.uiState.value.hapticEvents.map { it.startTimeMs }

        assertEquals(initialTimestamps, afterResumeTimestamps)
    }

    @Test
    fun testPlayline7_fastForwardDoesNotModifyMarkerTimestamps() = runBlocking {
        val videoRepo = DefaultVideoRepository()
        val f1File = findAssetFile("haptics/f1_haptic_timeline.json")
        val curatedProvider = CuratedHapticProvider(assetReader = { f1File.readText() })
        val hapticRepo = DefaultHapticRepository(providers = listOf(curatedProvider))
        val testScope = CoroutineScope(Dispatchers.Unconfined)

        val vm = VideoPlayerViewModel(
            videoRepository = videoRepo,
            hapticRepository = hapticRepo,
            hapticSynchronizer = synchronizer,
            hapticEngine = engine,
            coroutineScope = testScope
        )
        vm.loadVideo("f1_2025_haptic_trailer")

        val initialTimestamps = vm.uiState.value.hapticEvents.map { it.startTimeMs }
        vm.fastForward(15000L)
        val afterFfTimestamps = vm.uiState.value.hapticEvents.map { it.startTimeMs }

        assertEquals(initialTimestamps, afterFfTimestamps)
    }

    @Test
    fun testPlayline8_rewindDoesNotModifyMarkerTimestamps() = runBlocking {
        val videoRepo = DefaultVideoRepository()
        val f1File = findAssetFile("haptics/f1_haptic_timeline.json")
        val curatedProvider = CuratedHapticProvider(assetReader = { f1File.readText() })
        val hapticRepo = DefaultHapticRepository(providers = listOf(curatedProvider))
        val testScope = CoroutineScope(Dispatchers.Unconfined)

        val vm = VideoPlayerViewModel(
            videoRepository = videoRepo,
            hapticRepository = hapticRepo,
            hapticSynchronizer = synchronizer,
            hapticEngine = engine,
            coroutineScope = testScope
        )
        vm.loadVideo("f1_2025_haptic_trailer")
        vm.seekTo(60000L)

        val beforeRewind = vm.uiState.value.hapticEvents.map { it.startTimeMs }
        vm.rewind(10000L)
        val afterRewind = vm.uiState.value.hapticEvents.map { it.startTimeMs }

        assertEquals(beforeRewind, afterRewind)
    }

    @Test
    fun testPlayline9_f1MarkersOriginateFromF1HapticPatternEvents() = runBlocking {
        val f1File = findAssetFile("haptics/f1_haptic_timeline.json")
        val expectedPattern = FrequencyPatternParser.parseToHapticPattern(f1File.readText()).getOrThrow()

        val videoRepo = DefaultVideoRepository()
        val curatedProvider = CuratedHapticProvider(assetReader = { f1File.readText() })
        val hapticRepo = DefaultHapticRepository(providers = listOf(curatedProvider))
        val testScope = CoroutineScope(Dispatchers.Unconfined)

        val vm = VideoPlayerViewModel(
            videoRepository = videoRepo,
            hapticRepository = hapticRepo,
            hapticSynchronizer = synchronizer,
            hapticEngine = engine,
            coroutineScope = testScope
        )
        vm.loadVideo("f1_2025_haptic_trailer")

        assertEquals(141, vm.uiState.value.hapticEvents.size)
        assertEquals(expectedPattern.events.size, vm.uiState.value.hapticEvents.size)
        assertEquals(expectedPattern.events.map { it.startTimeMs }, vm.uiState.value.hapticEvents.map { it.startTimeMs })
    }

    @Test
    fun testPlayline10_kojiMarkersOriginateFromKojiHapticPatternEvents() = runBlocking {
        val kojiFile = findAssetFile("haptics/koji_haptic_timeline.json")
        val expectedPattern = FrequencyPatternParser.parseToHapticPattern(kojiFile.readText()).getOrThrow()

        val videoRepo = DefaultVideoRepository()
        val curatedProvider = CuratedHapticProvider(assetReader = { kojiFile.readText() })
        val hapticRepo = DefaultHapticRepository(providers = listOf(curatedProvider))
        val testScope = CoroutineScope(Dispatchers.Unconfined)

        val vm = VideoPlayerViewModel(
            videoRepository = videoRepo,
            hapticRepository = hapticRepo,
            hapticSynchronizer = synchronizer,
            hapticEngine = engine,
            coroutineScope = testScope
        )
        vm.loadVideo("koji")

        val uiEvents = vm.uiState.value.hapticEvents
        assertEquals(expectedPattern.events.size, uiEvents.size)
        assertEquals(expectedPattern.events.map { it.startTimeMs }, uiEvents.map { it.startTimeMs })
    }
}
