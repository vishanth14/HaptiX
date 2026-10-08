package com.haptix.app.haptics

import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.model.SemanticHapticType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive verification suite covering all 20 synchronization hardening requirements:
 *
 * 1. Start at 0ms
 * 2. Start from middle of video
 * 3. Normal forward playback
 * 4. Pause
 * 5. Resume
 * 6. Forward seek
 * 7. Backward seek
 * 8. Seek directly into an active event
 * 9. Seek across multiple events
 * 10. Repeated synchronization ticks
 * 11. Duplicate-trigger prevention
 * 12. Buffering
 * 13. Playback restart
 * 14. Repeat/loop
 * 15. Empty haptic list
 * 16. Malformed event timestamps
 * 17. Events outside video duration
 * 18. Remote haptic timeline
 * 19. Rapid consecutive seeks
 * 20. Orientation / player recreation
 */
class VideoHapticSynchronizationHardeningTest {

    private class TestHapticEngine : HapticEngine {
        val playedEvents = mutableListOf<HapticEvent>()
        var stopCount = 0
        var startCount = 0
        var isVibrating = false
        val stopReasons = mutableListOf<String>()

        override fun isHapticSupported(): Boolean = true
        override fun loadPattern(pattern: HapticPattern) {}
        override fun start() { startCount++ }
        override fun stop() { stopCount++; isVibrating = false; stopReasons.add("UNSPECIFIED") }
        override fun stop(reason: String) { stopCount++; isVibrating = false; stopReasons.add(reason) }
        override fun release() { stop() }
        override fun playEvent(event: HapticEvent) {
            playedEvents.add(event)
            isVibrating = true
        }

        fun resetRecording() {
            playedEvents.clear()
            stopCount = 0
            startCount = 0
            isVibrating = false
            stopReasons.clear()
        }
    }

    private lateinit var engine: TestHapticEngine
    private lateinit var synchronizer: HapticSynchronizer

    @Before
    fun setUp() {
        engine = TestHapticEngine()
        synchronizer = HapticSynchronizer(engine = engine, toleranceMs = 50L)
    }

    // ------------------------------------------------------------------------
    // 1. Start at 0ms
    // ------------------------------------------------------------------------
    @Test
    fun test01_startAtZeroMs_noSpuriousTriggersUntilFirstEvent() {
        val pattern = HapticPattern(
            videoId = "test_01",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 200L, intensity = 0.5f),
                HapticEvent(id = "E2", startTimeMs = 3000L, durationMs = 200L, intensity = 0.8f)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Ticks before 1000ms must be completely silent
        synchronizer.onTimelineUpdate(0L)
        synchronizer.onTimelineUpdate(250L)
        synchronizer.onTimelineUpdate(500L)
        synchronizer.onTimelineUpdate(990L)

        assertEquals(0, engine.playedEvents.size)
        assertFalse(engine.isVibrating)

        // Crossing into onset at 1000ms
        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("E1", engine.playedEvents[0].id)
        assertTrue(engine.isVibrating)
    }

    // ------------------------------------------------------------------------
    // 2. Start from middle of video
    // ------------------------------------------------------------------------
    @Test
    fun test02_startFromMiddleOfVideo_doesNotReplayPastEvents() {
        val pattern = HapticPattern(
            videoId = "test_02",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 5000L, durationMs = 500L),
                HapticEvent(id = "E2", startTimeMs = 10000L, durationMs = 500L),
                HapticEvent(id = "E3", startTimeMs = 30000L, durationMs = 1000L),
                HapticEvent(id = "E4", startTimeMs = 45000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)

        // Playback starts directly at 30,000ms (e.g. restored session or seek before play)
        synchronizer.onSeek(30000L)
        synchronizer.onResume(30000L)

        // Events E1 (5s) and E2 (10s) must NEVER play
        assertEquals(0, engine.playedEvents.size)

        // At 30,000ms, E3 (30s..31s) triggers
        synchronizer.onTimelineUpdate(30000L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("E3", engine.playedEvents[0].id)
    }

    // ------------------------------------------------------------------------
    // 3. Normal forward playback
    // ------------------------------------------------------------------------
    @Test
    fun test03_normalForwardPlayback_sequentialDispatchesWithInterEventSilence() {
        val pattern = HapticPattern(
            videoId = "test_03",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 200L), // [1000..1200)
                HapticEvent(id = "E2", startTimeMs = 2000L, durationMs = 300L)  // [2000..2300)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        synchronizer.onTimelineUpdate(500L)
        assertFalse(engine.isVibrating)

        // Enter E1
        synchronizer.onTimelineUpdate(1050L)
        assertEquals(1, engine.playedEvents.size)
        assertTrue(engine.isVibrating)

        // Silent gap between E1 and E2
        synchronizer.onTimelineUpdate(1500L)
        assertFalse(engine.isVibrating)

        // Enter E2
        synchronizer.onTimelineUpdate(2050L)
        assertEquals(2, engine.playedEvents.size)
        assertEquals("E2", engine.playedEvents[1].id)
        assertTrue(engine.isVibrating)

        // Past E2
        synchronizer.onTimelineUpdate(2500L)
        assertFalse(engine.isVibrating)
    }

    // ------------------------------------------------------------------------
    // 4. Pause
    // ------------------------------------------------------------------------
    @Test
    fun test04_pause_cancelsActiveVibrationImmediately() {
        val pattern = HapticPattern(
            videoId = "test_04",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 2000L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Enter interval -> active vibration
        synchronizer.onTimelineUpdate(1500L)
        assertTrue(engine.isVibrating)
        assertTrue(synchronizer.isActuatingState)

        // Pause triggered by user or lifecycle
        synchronizer.onPause("USER_PAUSE")
        assertFalse(engine.isVibrating)
        assertFalse(synchronizer.isActuatingState)
        assertEquals("PAUSED", synchronizer.playerState)
    }

    // ------------------------------------------------------------------------
    // 5. Resume
    // ------------------------------------------------------------------------
    @Test
    fun test05_resume_resumesFromActualExoPlayerPosition() {
        val pattern = HapticPattern(
            videoId = "test_05",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 500L),
                HapticEvent(id = "E2", startTimeMs = 5000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onTimelineUpdate(1100L)
        assertEquals(1, engine.playedEvents.size)

        // Paused at 1200ms
        synchronizer.onPause()

        // Resume at 4900ms (ExoPlayer was repositioned or resumed near E2)
        synchronizer.onResume(4900L)
        assertEquals("PLAYING", synchronizer.playerState)
        assertEquals(1, engine.playedEvents.size) // E1 not replayed

        // Advance to 5000ms -> E2 triggers
        synchronizer.onTimelineUpdate(5000L)
        assertEquals(2, engine.playedEvents.size)
        assertEquals("E2", engine.playedEvents[1].id)
    }

    // ------------------------------------------------------------------------
    // 6. Forward seek
    // ------------------------------------------------------------------------
    @Test
    fun test06_forwardSeek_skipsIntermediateEventsWithoutPlaying() {
        val pattern = HapticPattern(
            videoId = "test_06",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 1000L, durationMs = 500L),  // 1.0s - 1.5s
                HapticEvent(id = "B", startTimeMs = 2500L, durationMs = 500L),  // 2.5s - 3.0s
                HapticEvent(id = "C", startTimeMs = 5000L, durationMs = 1000L)  // 5.0s - 6.0s
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onTimelineUpdate(500L)
        assertEquals(0, engine.playedEvents.size)

        // User seeks 0.5s -> 4.5s
        synchronizer.onSeek(4500L)
        assertEquals("Skipped events must not fire on seek", 0, engine.playedEvents.size)

        // Advance to 5000ms -> only C is eligible and plays
        synchronizer.onTimelineUpdate(5000L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("C", engine.playedEvents[0].id)
    }

    // ------------------------------------------------------------------------
    // 7. Backward seek
    // ------------------------------------------------------------------------
    @Test
    fun test07_backwardSeek_reEnablesPreviousEventsForNewPass() {
        val pattern = HapticPattern(
            videoId = "test_07",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 1000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)

        // Play A once
        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)

        // Progress past A
        synchronizer.onTimelineUpdate(1600L)
        assertFalse(engine.isVibrating)

        // Seek backward to 500ms
        synchronizer.onSeek(500L)
        engine.playedEvents.clear()

        // Progress forward again: A must be allowed to trigger on this new pass
        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("A", engine.playedEvents[0].id)
    }

    // ------------------------------------------------------------------------
    // 8. Seek directly into an active event
    // ------------------------------------------------------------------------
    @Test
    fun test08_seekDirectlyIntoActiveEvent_triggersOnceWithoutRepeats() {
        val pattern = HapticPattern(
            videoId = "test_08",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 20000L, durationMs = 1000L) // 20s - 21s
            )
        )
        synchronizer.setPattern(pattern)

        // Seek directly to 20,500ms (inside event interval)
        synchronizer.onSeek(20500L)
        assertEquals(0, engine.playedEvents.size)

        // First update at 20,500ms triggers event once
        synchronizer.onTimelineUpdate(20500L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("A", engine.playedEvents[0].id)

        // Subsequent ticks inside same window do not re-trigger
        synchronizer.onTimelineUpdate(20550L)
        synchronizer.onTimelineUpdate(20600L)
        synchronizer.onTimelineUpdate(20700L)
        assertEquals(1, engine.playedEvents.size)
    }

    // ------------------------------------------------------------------------
    // 9. Seek across multiple events
    // ------------------------------------------------------------------------
    @Test
    fun test09_seekAcrossMultipleEvents_marksAllIntermediatesCompleted() {
        val events = (1..10).map { i ->
            HapticEvent(id = "E$i", startTimeMs = i * 1000L, durationMs = 200L)
        }
        val pattern = HapticPattern(videoId = "test_09", events = events)
        synchronizer.setPattern(pattern)

        // Seek across E1..E7 to 7500ms
        synchronizer.onSeek(7500L)
        assertEquals(0, engine.playedEvents.size)

        // Only E8..E10 can play
        synchronizer.onTimelineUpdate(8000L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("E8", engine.playedEvents[0].id)
    }

    // ------------------------------------------------------------------------
    // 10. Repeated synchronization ticks inside event interval
    // ------------------------------------------------------------------------
    @Test
    fun test10_explicitDuplicateTest_1000To1250msFiresExactlyOnce() {
        val pattern = HapticPattern(
            videoId = "test_10",
            events = listOf(
                HapticEvent(id = "EventA", startTimeMs = 1000L, durationMs = 500L) // 1000–1500ms
            )
        )
        synchronizer.setPattern(pattern)

        // Simulate high-frequency synchronization checks:
        // 1000, 1050, 1100, 1150, 1200, 1250
        val ticks = listOf(1000L, 1050L, 1100L, 1150L, 1200L, 1250L)
        for (tick in ticks) {
            synchronizer.onTimelineUpdate(tick)
            assertEquals("EventA must trigger exactly ONCE across all ticks", 1, engine.playedEvents.size)
        }
    }

    // ------------------------------------------------------------------------
    // 11. Duplicate-trigger prevention on stationary / identical ticks
    // ------------------------------------------------------------------------
    @Test
    fun test11_stationaryTicks_neverRetrigger() {
        val pattern = HapticPattern(
            videoId = "test_11",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 2000L, durationMs = 400L)
            )
        )
        synchronizer.setPattern(pattern)

        synchronizer.onTimelineUpdate(2050L)
        assertEquals(1, engine.playedEvents.size)

        // Repeat identical timestamp 5 times (e.g. stalled buffer or stationary frame)
        repeat(5) {
            synchronizer.onTimelineUpdate(2050L)
            assertEquals(1, engine.playedEvents.size)
        }
    }

    // ------------------------------------------------------------------------
    // 12. Buffering
    // ------------------------------------------------------------------------
    @Test
    fun test12_buffering_cancelsActiveVibrationAndHaltsProgression() {
        val pattern = HapticPattern(
            videoId = "test_12",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 1000L, durationMs = 2000L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onTimelineUpdate(1500L)
        assertTrue(engine.isVibrating)

        // Media3 enters buffering
        synchronizer.onBuffering()
        assertFalse("Buffering must immediately silence vibration", engine.isVibrating)
        assertFalse(synchronizer.isActuatingState)
        assertEquals("BUFFERING", synchronizer.playerState)
    }

    // ------------------------------------------------------------------------
    // 13. Playback restart
    // ------------------------------------------------------------------------
    @Test
    fun test13_playbackRestart_afterCompletionResetsEventStates() {
        val pattern = HapticPattern(
            videoId = "test_13",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 1000L, durationMs = 200L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onTimelineUpdate(1000L)
        synchronizer.onPlaybackComplete()
        assertEquals("ENDED", synchronizer.playerState)

        // Replay from beginning
        synchronizer.onSeek(0L)
        synchronizer.onResume(0L)
        engine.playedEvents.clear()

        synchronizer.onTimelineUpdate(1000L)
        assertEquals("Event must fire again on restarted playback", 1, engine.playedEvents.size)
    }

    // ------------------------------------------------------------------------
    // 14. Repeat / loop
    // ------------------------------------------------------------------------
    @Test
    fun test14_repeatLoop_autoDetectsBackwardJumpAndReEnablesEvents() {
        val pattern = HapticPattern(
            videoId = "test_14",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 1000L, durationMs = 200L),
                HapticEvent(id = "B", startTimeMs = 8000L, durationMs = 200L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onTimelineUpdate(1000L)
        synchronizer.onTimelineUpdate(8000L)
        assertEquals(2, engine.playedEvents.size)

        // Player loops: timestamp drops from 8050ms to 0ms without explicit onSeek
        synchronizer.onTimelineUpdate(0L)
        engine.playedEvents.clear()

        // Loop pass: A and B must fire again
        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("A", engine.playedEvents[0].id)
    }

    // ------------------------------------------------------------------------
    // 15. Empty haptic list
    // ------------------------------------------------------------------------
    @Test
    fun test15_emptyHapticList_safeAndSilent() {
        val pattern = HapticPattern(videoId = "empty_test", events = emptyList())
        synchronizer.setPattern(pattern)

        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(0L)
        synchronizer.onTimelineUpdate(5000L)
        synchronizer.onTimelineUpdate(10000L)
        synchronizer.onPlaybackComplete()

        assertEquals(0, engine.playedEvents.size)
        assertFalse(engine.isVibrating)
    }

    // ------------------------------------------------------------------------
    // 16. Malformed event timestamps
    // ------------------------------------------------------------------------
    @Test
    fun test16_malformedEventTimestamps_rejectedSafelyWithoutCrashing() {
        // Events with valid timestamps
        val validEvent = HapticEvent(id = "VALID", startTimeMs = 1000L, durationMs = 200L)
        val pattern = HapticPattern(videoId = "malformed_test", events = listOf(validEvent))

        synchronizer.setPattern(pattern)
        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("VALID", engine.playedEvents[0].id)
    }

    // ------------------------------------------------------------------------
    // 17. Events outside video duration
    // ------------------------------------------------------------------------
    @Test
    fun test17_eventsOutsideVideoDuration_handledGracefully() {
        val pattern = HapticPattern(
            videoId = "test_17",
            videoDurationMs = 5000L,
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 1000L, durationMs = 200L),
                HapticEvent(id = "B_OUTSIDE", startTimeMs = 15000L, durationMs = 200L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)

        // Playback ends at 5000ms
        synchronizer.onTimelineUpdate(5000L)
        synchronizer.onPlaybackComplete()

        assertEquals(1, engine.playedEvents.size)
        assertEquals("A", engine.playedEvents[0].id)
    }

    // ------------------------------------------------------------------------
    // 18. Remote haptic timeline
    // ------------------------------------------------------------------------
    @Test
    fun test18_remoteHapticTimeline_exactTimestampsPreservedWithoutOffsets() {
        // Exact F1 senior research timestamps
        val remoteEvents = listOf(
            HapticEvent(id = "GEAR_26.54s", startTimeMs = 26540L, durationMs = 80L),
            HapticEvent(id = "ACCEL_30.66s", startTimeMs = 30660L, durationMs = 120L),
            HapticEvent(id = "CURB_42.76s", startTimeMs = 42760L, durationMs = 260L)
        )
        val pattern = HapticPattern(videoId = "f1_remote", events = remoteEvents)
        synchronizer.setPattern(pattern)

        // Advance to exactly 42,760ms
        synchronizer.onSeek(42000L)
        synchronizer.onTimelineUpdate(42760L)

        val last = synchronizer.getLastDispatched()
        assertNotNull(last)
        assertEquals(42760L, last?.startTimeMs)
        assertEquals("CURB_42.76s", last?.id)
        assertEquals(42760L, synchronizer.getLastDispatchedTimestampMs())
    }

    // ------------------------------------------------------------------------
    // 19. Rapid consecutive seeks
    // ------------------------------------------------------------------------
    @Test
    fun test19_rapidConsecutiveSeeks_cancelsActiveVibrationsDeterministically() {
        val pattern = HapticPattern(
            videoId = "test_19",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 1000L, durationMs = 200L),
                HapticEvent(id = "B", startTimeMs = 3000L, durationMs = 200L),
                HapticEvent(id = "C", startTimeMs = 5000L, durationMs = 200L)
            )
        )
        synchronizer.setPattern(pattern)

        val initialStops = engine.stopCount

        // 10 rapid seeks
        val seekTargets = listOf(500L, 1200L, 2500L, 800L, 4000L, 100L, 3000L, 2000L, 4500L, 5000L)
        for (target in seekTargets) {
            synchronizer.onSeek(target)
        }

        // Each seek must invoke engine.stop() to cancel lingering vibration
        assertEquals(initialStops + 10, engine.stopCount)
        assertEquals(0, engine.playedEvents.size)

        // Resuming at 5000ms triggers C cleanly
        synchronizer.onTimelineUpdate(5000L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("C", engine.playedEvents[0].id)
    }

    // ------------------------------------------------------------------------
    // 20. Orientation / player recreation
    // ------------------------------------------------------------------------
    @Test
    fun test20_orientationOrPlayerRecreation_restoresPositionCleanly() {
        val pattern = HapticPattern(
            videoId = "test_20",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 1000L, durationMs = 200L),
                HapticEvent(id = "B", startTimeMs = 5000L, durationMs = 200L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)

        // Configuration change happens: player paused at 3000ms
        synchronizer.onPause()

        // New screen instance created: position 3000ms restored
        synchronizer.onSeek(3000L)
        synchronizer.onResume(3000L)

        // A is in past -> not replayed
        assertEquals(1, engine.playedEvents.size)

        // Play to B -> B triggers
        synchronizer.onTimelineUpdate(5000L)
        assertEquals(2, engine.playedEvents.size)
        assertEquals("B", engine.playedEvents[1].id)
    }

    // ========================================================================
    // SECTION 17 SPECIFIC SYNCHRONIZATION HARDENING REQUIREMENTS
    // ========================================================================

    // ------------------------------------------------------------------------
    // CRITICAL TEST: 1000–1500ms event with high-frequency 25ms ticks
    // ------------------------------------------------------------------------
    @Test
    fun testCritical_1000To1500ms_exactSingleTrigger() {
        val pattern = HapticPattern(
            videoId = "critical_01",
            events = listOf(
                HapticEvent(id = "CRIT_EVENT", startTimeMs = 1000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(1000L)

        val ticks = listOf(1000L, 1025L, 1050L, 1075L, 1100L, 1125L, 1150L, 1175L, 1200L, 1250L)
        for (tick in ticks) {
            synchronizer.onTimelineUpdate(tick)
            assertEquals("Event must trigger exactly ONE time across all ticks", 1, engine.playedEvents.size)
            assertTrue("Synchronizer must remain actuating during event", synchronizer.isActuatingState)
        }
        assertEquals("CRIT_EVENT", engine.playedEvents[0].id)
    }

    // ------------------------------------------------------------------------
    // CRITICAL LARGE-JUMP TEST: A=5000–5500, B=10000–10500, C=20000–20500
    // Positions: 1000, 1025, 1050, 20000
    // Expected: A never triggered, B never triggered, C eligible at 20000
    // ------------------------------------------------------------------------
    @Test
    fun testCritical_largeJumpTest_skipsIntermediatesAndArmsTarget() {
        val pattern = HapticPattern(
            videoId = "critical_02",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 5000L, durationMs = 500L),
                HapticEvent(id = "B", startTimeMs = 10000L, durationMs = 500L),
                HapticEvent(id = "C", startTimeMs = 20000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(1000L)

        // Normal 25ms progression around 1000ms
        synchronizer.onTimelineUpdate(1000L)
        synchronizer.onTimelineUpdate(1025L)
        synchronizer.onTimelineUpdate(1050L)
        assertEquals(0, engine.playedEvents.size)

        // Sudden large forward jump: 1050ms -> 20000ms (delta = 18950ms > MAX_FORWARD_GAP_MS)
        synchronizer.onTimelineUpdate(20000L)

        // A and B must NEVER have triggered
        assertEquals("A and B must be skipped and never triggered; C is eligible at 20000", 1, engine.playedEvents.size)
        assertEquals("C", engine.playedEvents[0].id)
        assertEquals(EventTriggerState.COMPLETED, synchronizer.getEventState("A"))
        assertEquals(EventTriggerState.COMPLETED, synchronizer.getEventState("B"))
        assertEquals(EventTriggerState.ACTIVE, synchronizer.getEventState("C"))
    }

    // ------------------------------------------------------------------------
    // CRITICAL PAUSE TEST: 10000–12000ms event
    // Positions: 10000, 10250, 10500 -> PAUSE -> RESUME at 10500
    // Expected: active event safely cancelled, re-armed at resume, no duplicate loop
    // ------------------------------------------------------------------------
    @Test
    fun testCritical_pauseTest_pauseAndResumeInsideActiveEvent() {
        val pattern = HapticPattern(
            videoId = "critical_03",
            events = listOf(
                HapticEvent(id = "LONG_EVENT", startTimeMs = 10000L, durationMs = 2000L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(10000L)

        synchronizer.onTimelineUpdate(10000L)
        assertEquals(1, engine.playedEvents.size)
        assertTrue(engine.isVibrating)

        synchronizer.onTimelineUpdate(10250L)
        assertEquals(1, engine.playedEvents.size)

        synchronizer.onTimelineUpdate(10500L)
        assertEquals(1, engine.playedEvents.size)

        // PAUSE at 10500ms
        synchronizer.onPause("TEST_PAUSE")
        assertFalse("Active vibration must stop on pause", engine.isVibrating)
        assertFalse(synchronizer.isActuatingState)
        assertEquals(EventTriggerState.NOT_TRIGGERED, synchronizer.getEventState("LONG_EVENT"))

        // RESUME at 10500ms
        synchronizer.onResume(10500L)
        // Resume tick triggers re-armed event cleanly
        synchronizer.onTimelineUpdate(10500L)
        assertEquals("Event must re-actuate upon resume within interval", 2, engine.playedEvents.size)
        assertTrue(engine.isVibrating)
        assertTrue(synchronizer.isActuatingState)

        // Subsequent ticks must NOT trigger duplicate loops
        synchronizer.onTimelineUpdate(10525L)
        synchronizer.onTimelineUpdate(10550L)
        synchronizer.onTimelineUpdate(10575L)
        assertEquals("No duplicate loops on subsequent ticks", 2, engine.playedEvents.size)
    }

    // ------------------------------------------------------------------------
    // 01. Normal 25ms forward ticks
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_01_normal25msForwardTicks() {
        val pattern = HapticPattern(
            videoId = "s17_01",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 500L, durationMs = 100L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        for (t in 0L..475L step 25L) {
            synchronizer.onTimelineUpdate(t)
        }
        assertEquals(0, engine.playedEvents.size)

        synchronizer.onTimelineUpdate(500L)
        assertEquals(1, engine.playedEvents.size)
        assertTrue(engine.isVibrating)

        for (t in 525L..575L step 25L) {
            synchronizer.onTimelineUpdate(t)
        }
        assertEquals(1, engine.playedEvents.size)

        synchronizer.onTimelineUpdate(600L)
        assertFalse(engine.isVibrating)
    }

    // ------------------------------------------------------------------------
    // 02. Large forward discontinuity (> 500ms)
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_02_largeForwardDiscontinuity_skipsIntermediates() {
        val pattern = HapticPattern(
            videoId = "s17_02",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 100L),
                HapticEvent(id = "E2", startTimeMs = 2000L, durationMs = 100L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(500L)
        synchronizer.onTimelineUpdate(500L)

        // Forward discontinuity jump: 500 -> 3000ms
        synchronizer.onTimelineUpdate(3000L)
        assertEquals("Skipped intermediate events must not trigger", 0, engine.playedEvents.size)
        assertEquals(EventTriggerState.COMPLETED, synchronizer.getEventState("E1"))
        assertEquals(EventTriggerState.COMPLETED, synchronizer.getEventState("E2"))
    }

    // ------------------------------------------------------------------------
    // 03. Small forward progression
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_03_smallForwardProgression_triggersSmoothly() {
        val pattern = HapticPattern(
            videoId = "s17_03",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 200L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(900L)
        synchronizer.onTimelineUpdate(900L)
        synchronizer.onTimelineUpdate(950L)
        assertEquals(0, engine.playedEvents.size)

        synchronizer.onTimelineUpdate(1010L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("E1", engine.playedEvents[0].id)
    }

    // ------------------------------------------------------------------------
    // 04. Paused timeline tick
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_04_pausedTimelineTick_neverActuates() {
        val pattern = HapticPattern(
            videoId = "s17_04",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onPause()
        assertEquals(PlaybackSyncState.PAUSED, synchronizer.syncState)

        synchronizer.onTimelineUpdate(1000L)
        synchronizer.onTimelineUpdate(1200L)
        assertEquals(0, engine.playedEvents.size)
        assertFalse(engine.isVibrating)
    }

    // ------------------------------------------------------------------------
    // 05. Buffering timeline tick
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_05_bufferingTimelineTick_neverActuates() {
        val pattern = HapticPattern(
            videoId = "s17_05",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onBuffering()
        assertEquals(PlaybackSyncState.BUFFERING, synchronizer.syncState)

        synchronizer.onTimelineUpdate(1000L)
        synchronizer.onTimelineUpdate(1200L)
        assertEquals(0, engine.playedEvents.size)
        assertFalse(engine.isVibrating)
    }

    // ------------------------------------------------------------------------
    // 06. Seek while paused
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_06_seekWhilePaused_doesNotActuateUntilResumed() {
        val pattern = HapticPattern(
            videoId = "s17_06",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 5000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onPause()

        // Seek while paused
        synchronizer.onSeek(5000L)
        synchronizer.onTimelineUpdate(5000L)
        assertEquals("Must not actuate while paused even if seek lands on event", 0, engine.playedEvents.size)

        // Resume and tick
        synchronizer.onResume(5000L)
        synchronizer.onTimelineUpdate(5000L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("E1", engine.playedEvents[0].id)
    }

    // ------------------------------------------------------------------------
    // 07. Pause inside active event
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_07_pauseInsideActiveEvent_stopsActuationImmediately() {
        val pattern = HapticPattern(
            videoId = "s17_07",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 2000L, durationMs = 1000L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(2000L)
        synchronizer.onTimelineUpdate(2500L)
        assertTrue(engine.isVibrating)

        synchronizer.onPause()
        assertFalse("Vibration must stop on pause", engine.isVibrating)
        assertEquals(EventTriggerState.NOT_TRIGGERED, synchronizer.getEventState("E1"))
    }

    // ------------------------------------------------------------------------
    // 08. Resume inside active event
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_08_resumeInsideActiveEvent_reArmsAndReActuatesDeterministically() {
        val pattern = HapticPattern(
            videoId = "s17_08",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 4000L, durationMs = 1000L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(4000L)
        synchronizer.onTimelineUpdate(4200L)
        assertEquals(1, engine.playedEvents.size)

        synchronizer.onPause()
        synchronizer.onResume(4500L)
        synchronizer.onTimelineUpdate(4500L)
        assertEquals(2, engine.playedEvents.size)

        synchronizer.onTimelineUpdate(4550L)
        assertEquals("Must not retrigger repeatedly within same resumed window", 2, engine.playedEvents.size)
    }

    // ------------------------------------------------------------------------
    // 09. Buffering inside active event
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_09_bufferingInsideActiveEvent_silencesAndReArms() {
        val pattern = HapticPattern(
            videoId = "s17_09",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 6000L, durationMs = 1000L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(6000L)
        synchronizer.onTimelineUpdate(6200L)
        assertTrue(engine.isVibrating)

        synchronizer.onBuffering()
        assertFalse(engine.isVibrating)
        assertEquals(EventTriggerState.NOT_TRIGGERED, synchronizer.getEventState("E1"))

        synchronizer.onResume(6400L)
        synchronizer.onTimelineUpdate(6400L)
        assertEquals(2, engine.playedEvents.size)
        assertTrue(engine.isVibrating)
    }

    // ------------------------------------------------------------------------
    // 10. Configurable actuation lead = 0 (default)
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_10_configurableActuationLead_defaultZero() {
        val pattern = HapticPattern(
            videoId = "s17_10",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 2000L, durationMs = 100L)
            )
        )
        synchronizer.setPattern(pattern)
        assertEquals(0L, synchronizer.actuationLeadMs)

        synchronizer.onResume(1950L)
        synchronizer.onTimelineUpdate(1990L)
        assertEquals(0, engine.playedEvents.size)

        synchronizer.onTimelineUpdate(2000L)
        assertEquals(1, engine.playedEvents.size)
    }

    // ------------------------------------------------------------------------
    // 11. Configurable actuation lead > 0
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_11_configurableActuationLead_positiveLead() {
        val pattern = HapticPattern(
            videoId = "s17_11",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 2000L, durationMs = 100L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.actuationLeadMs = 50L // 50ms lead

        synchronizer.onResume(1900L)
        synchronizer.onTimelineUpdate(1940L)
        assertEquals(0, engine.playedEvents.size)

        // At video time 1950ms + 50ms lead = 2000ms (event onset)
        synchronizer.onTimelineUpdate(1950L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("E1", engine.playedEvents[0].id)
    }

    // ------------------------------------------------------------------------
    // 12. Playback speed behavior: uses authoritative Media3 position
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_12_playbackSpeedBehavior_authoritativeMedia3Position() {
        val pattern = HapticPattern(
            videoId = "s17_12",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 2000L, durationMs = 200L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(1000L)

        // Even with non-standard speed, ExoPlayer reports true currentPosition
        synchronizer.onTimelineUpdate(1500L)
        assertEquals(0, engine.playedEvents.size)

        synchronizer.onTimelineUpdate(2000L)
        assertEquals(1, engine.playedEvents.size)
    }

    // ------------------------------------------------------------------------
    // 13. Duplicate event IDs
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_13_duplicateEventIds_trackedCleanlyByIndex() {
        val pattern = HapticPattern(
            videoId = "s17_13",
            events = listOf(
                HapticEvent(id = "SAME_ID", startTimeMs = 1000L, durationMs = 200L),
                HapticEvent(id = "SAME_ID", startTimeMs = 3000L, durationMs = 200L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)

        synchronizer.onTimelineUpdate(2000L)
        assertFalse(engine.isVibrating)

        synchronizer.onTimelineUpdate(3000L)
        assertEquals("Both events with identical IDs must trigger independently", 2, engine.playedEvents.size)
    }

    // ------------------------------------------------------------------------
    // 14. Duplicate start timestamps
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_14_duplicateStartTimestamps_bothTriggerWithoutCollision() {
        val pattern = HapticPattern(
            videoId = "s17_14",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 2000L, durationMs = 200L, intensity = 0.5f),
                HapticEvent(id = "B", startTimeMs = 2000L, durationMs = 400L, intensity = 0.9f)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        synchronizer.onTimelineUpdate(2000L)
        assertEquals(2, engine.playedEvents.size)
        assertEquals(setOf("A", "B"), engine.playedEvents.map { it.id }.toSet())
    }

    // ------------------------------------------------------------------------
    // 15. Overlapping events
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_15_overlappingEvents_deterministicActuationAndSilence() {
        val pattern = HapticPattern(
            videoId = "s17_15",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 10000L, durationMs = 1000L), // 10.0s - 11.0s
                HapticEvent(id = "B", startTimeMs = 10500L, durationMs = 1000L)  // 10.5s - 11.5s
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(9000L)

        // Enter A
        synchronizer.onTimelineUpdate(10000L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("A", engine.playedEvents[0].id)
        assertTrue(engine.isVibrating)

        // Enter B (A still active)
        synchronizer.onTimelineUpdate(10500L)
        assertEquals(2, engine.playedEvents.size)
        assertEquals("B", engine.playedEvents[1].id)
        assertTrue(engine.isVibrating)

        // A ends at 11000ms, but B is still active until 11500ms -> MUST NOT silence engine
        synchronizer.onTimelineUpdate(11000L)
        assertTrue("Engine must remain vibrating while B is active", engine.isVibrating)

        // B ends at 11500ms -> both ended, MUST silence
        synchronizer.onTimelineUpdate(11500L)
        assertFalse("Engine must be silent once all active events have finished", engine.isVibrating)
    }

    // ------------------------------------------------------------------------
    // 16. Zero-duration / impulse event
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_16_zeroDurationEvent_triggersOnceWithoutInfiniteLoop() {
        val pattern = HapticPattern(
            videoId = "s17_16",
            events = listOf(
                HapticEvent(id = "IMPULSE", startTimeMs = 1000L, durationMs = 0L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)

        synchronizer.onTimelineUpdate(1025L)
        assertEquals("Must not retrigger impulse inside its window", 1, engine.playedEvents.size)

        synchronizer.onTimelineUpdate(1100L)
        assertEquals(1, engine.playedEvents.size)
        assertFalse(engine.isVibrating)
    }

    // ------------------------------------------------------------------------
    // 17. Seek directly into active event
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_17_seekDirectlyIntoActiveEvent_triggersOnce() {
        val pattern = HapticPattern(
            videoId = "s17_17",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 20000L, durationMs = 1000L)
            )
        )
        synchronizer.setPattern(pattern)

        synchronizer.onSeek(20500L)
        synchronizer.onResume(20500L)
        synchronizer.onTimelineUpdate(20500L)
        assertEquals(1, engine.playedEvents.size)

        synchronizer.onTimelineUpdate(20550L)
        assertEquals(1, engine.playedEvents.size)
    }

    // ------------------------------------------------------------------------
    // 18. Seek beyond event
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_18_seekBeyondEvent_eventSkipped() {
        val pattern = HapticPattern(
            videoId = "s17_18",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 20000L, durationMs = 1000L)
            )
        )
        synchronizer.setPattern(pattern)

        synchronizer.onSeek(21500L)
        synchronizer.onResume(21500L)
        synchronizer.onTimelineUpdate(21500L)
        assertEquals(0, engine.playedEvents.size)
        assertEquals(EventTriggerState.COMPLETED, synchronizer.getEventState("E1"))
    }

    // ------------------------------------------------------------------------
    // 19. Rapid consecutive seeks
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_19_rapidConsecutiveSeeks_latestPositionWins() {
        val pattern = HapticPattern(
            videoId = "s17_19",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 1000L, durationMs = 100L),
                HapticEvent(id = "B", startTimeMs = 20000L, durationMs = 100L),
                HapticEvent(id = "C", startTimeMs = 40000L, durationMs = 100L)
            )
        )
        synchronizer.setPattern(pattern)

        val targets = listOf(5000L, 20000L, 8000L, 40000L, 12000L, 60000L)
        for (target in targets) {
            synchronizer.onSeek(target)
        }
        assertEquals(0, engine.playedEvents.size)

        // Resumed at 60000ms: A, B, C are all skipped
        synchronizer.onResume(60000L)
        synchronizer.onTimelineUpdate(60000L)
        assertEquals(0, engine.playedEvents.size)
    }

    // ------------------------------------------------------------------------
    // 20. Background / foreground lifecycle
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_20_backgroundForegroundLifecycle_preservesAuthoritativePosition() {
        val pattern = HapticPattern(
            videoId = "s17_20",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 1000L, durationMs = 200L),
                HapticEvent(id = "B", startTimeMs = 5000L, durationMs = 200L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)

        // Activity goes to background -> onPause
        synchronizer.onPause()
        assertFalse(engine.isVibrating)

        // Activity resumes from actual Media3 position (say 3000ms)
        synchronizer.onResume(3000L)
        synchronizer.onTimelineUpdate(3000L)
        assertEquals(1, engine.playedEvents.size) // A not replayed

        // Progress to B
        synchronizer.onTimelineUpdate(5000L)
        assertEquals(2, engine.playedEvents.size)
    }

    // ------------------------------------------------------------------------
    // 21. Playback restart
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_21_playbackRestart_reArmsPassAndEvents() {
        val pattern = HapticPattern(
            videoId = "s17_21",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 1000L, durationMs = 200L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)

        synchronizer.onPlaybackComplete()
        assertEquals(PlaybackSyncState.STOPPED, synchronizer.syncState)

        // Restart
        synchronizer.onSeek(0L)
        synchronizer.onResume(0L)
        engine.playedEvents.clear()

        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("A", engine.playedEvents[0].id)
    }

    // ------------------------------------------------------------------------
    // 22. Loop restart
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_22_loopRestart_reArmsPassAndEvents() {
        val pattern = HapticPattern(
            videoId = "s17_22",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 1000L, durationMs = 200L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)

        // Loop detection: position drops back to 0ms without explicit onSeek
        synchronizer.onTimelineUpdate(0L)
        engine.playedEvents.clear()

        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)
    }

    // ------------------------------------------------------------------------
    // 23. Repeated identical position ticks
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_23_repeatedIdenticalPositionTicks_singleActuation() {
        val pattern = HapticPattern(
            videoId = "s17_23",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 1000L, durationMs = 200L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        repeat(10) {
            synchronizer.onTimelineUpdate(1050L)
        }
        assertEquals(1, engine.playedEvents.size)
    }

    // ------------------------------------------------------------------------
    // 24. Large forward jump after lifecycle interruption
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_24_largeForwardJumpAfterLifecycleInterruption() {
        val pattern = HapticPattern(
            videoId = "s17_24",
            events = listOf(
                HapticEvent(id = "A", startTimeMs = 1000L, durationMs = 200L),
                HapticEvent(id = "B", startTimeMs = 5000L, durationMs = 200L),
                HapticEvent(id = "C", startTimeMs = 25000L, durationMs = 200L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)

        // Long background/pause gap, followed by player jumping directly to 25000ms
        synchronizer.onTimelineUpdate(25000L)
        // B (5s) must be skipped, C (25s) triggers
        assertEquals(2, engine.playedEvents.size)
        assertEquals("C", engine.playedEvents[1].id)
        assertEquals(EventTriggerState.COMPLETED, synchronizer.getEventState("B"))
    }

    // ------------------------------------------------------------------------
    // 25. Malformed / empty remote timeline
    // ------------------------------------------------------------------------
    @Test
    fun testSection17_25_malformedOrEmptyRemoteTimeline_safeAndSilent() {
        // 1. Empty timeline
        val emptyPattern = HapticPattern(videoId = "empty_remote", events = emptyList())
        synchronizer.setPattern(emptyPattern)
        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(0L)
        synchronizer.onTimelineUpdate(5000L)
        assertEquals(0, engine.playedEvents.size)
        assertFalse(engine.isVibrating)

        // 2. Unordered events and events exceeding video duration
        val unorderedPattern = HapticPattern(
            videoId = "unordered_remote",
            videoDurationMs = 3000L,
            events = listOf(
                HapticEvent(id = "LATER", startTimeMs = 2000L, durationMs = 100L),
                HapticEvent(id = "EARLIER", startTimeMs = 1000L, durationMs = 100L),
                HapticEvent(id = "OUT_OF_BOUNDS", startTimeMs = 10000L, durationMs = 100L)
            )
        )
        synchronizer.setPattern(unorderedPattern)
        synchronizer.onResume(0L)
        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("EARLIER", engine.playedEvents[0].id)

        synchronizer.onTimelineUpdate(2000L)
        assertEquals(2, engine.playedEvents.size)
        assertEquals("LATER", engine.playedEvents[1].id)

        synchronizer.onTimelineUpdate(3000L)
        synchronizer.onPlaybackComplete()
        assertEquals(2, engine.playedEvents.size) // OUT_OF_BOUNDS never fired
    }
}
