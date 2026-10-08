package com.haptix.app.haptics

import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticEventType
import com.haptix.app.data.model.HapticPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HapticSynchronizerTest {

    private class RecordingHapticEngine : HapticEngine {
        val playedEvents = mutableListOf<HapticEvent>()
        var stopCount = 0
        var startCount = 0
        var isVibrating = false

        override fun isHapticSupported(): Boolean = true
        override fun loadPattern(pattern: HapticPattern) {}
        override fun start() { startCount++ }
        override fun stop() { stopCount++; isVibrating = false }
        override fun stop(reason: String) { stopCount++; isVibrating = false }
        override fun release() { stop() }
        override fun playEvent(event: HapticEvent) {
            playedEvents.add(event)
            isVibrating = true
        }
    }

    private lateinit var engine: RecordingHapticEngine
    private lateinit var synchronizer: HapticSynchronizer
    private lateinit var pattern: HapticPattern

    @Before
    fun setUp() {
        engine = RecordingHapticEngine()
        synchronizer = HapticSynchronizer(engine = engine, toleranceMs = 50L)

        val events = listOf(
            HapticEvent(startTimeMs = 1000L, durationMs = 100L, intensity = 0.5f),
            HapticEvent(startTimeMs = 2500L, durationMs = 80L, intensity = 0.8f),
            HapticEvent(startTimeMs = 5000L, durationMs = 200L, intensity = 1.0f),
            HapticEvent(startTimeMs = 8000L, durationMs = 150L, intensity = 0.7f)
        )
        pattern = HapticPattern(videoId = "vid_sync_test", events = events)
        synchronizer.setPattern(pattern)
    }

    @Test
    fun forwardPlayback_dispatchesEachEventOnce() {
        // Step before event 1
        synchronizer.onTimelineUpdate(500L)
        assertEquals(0, engine.playedEvents.size)

        // Step within event 1 window [1000..1050]
        synchronizer.onTimelineUpdate(1020L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals(1000L, engine.playedEvents[0].startTimeMs)

        // Subsequence ticks in same window do not duplicate
        synchronizer.onTimelineUpdate(1030L)
        synchronizer.onTimelineUpdate(1040L)
        assertEquals(1, engine.playedEvents.size)

        // Step across event 2 (jump from 2400 to 2600)
        synchronizer.onTimelineUpdate(2400L)
        synchronizer.onTimelineUpdate(2600L)
        assertEquals(2, engine.playedEvents.size)
        assertEquals(2500L, engine.playedEvents[1].startTimeMs)
    }

    @Test
    fun seekForward_skipsPastEventsWithoutPlaying() {
        // Seek to 4000ms (skipping 1000ms and 2500ms events)
        synchronizer.onSeek(4000L)
        assertEquals(0, engine.playedEvents.size)
        assertTrue(engine.stopCount >= 1)

        // Play past 5000ms event
        synchronizer.onTimelineUpdate(5020L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals(5000L, engine.playedEvents[0].startTimeMs)
    }

    @Test
    fun seekBackward_restoresEventEligibility() {
        // Play first two events
        synchronizer.onTimelineUpdate(1020L)
        synchronizer.onTimelineUpdate(2520L)
        assertEquals(2, engine.playedEvents.size)

        // Seek backward to 500ms
        synchronizer.onSeek(500L)
        engine.playedEvents.clear()

        // Play forward again: 1000ms event must trigger again
        synchronizer.onTimelineUpdate(1010L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals(1000L, engine.playedEvents[0].startTimeMs)
    }

    @Test
    fun rapidScrubbing_remainsStableAndCancelsVibration() {
        val stopBefore = engine.stopCount

        // Rapid seeks back and forth
        synchronizer.onSeek(3000L)
        synchronizer.onSeek(1000L)
        synchronizer.onSeek(7000L)
        synchronizer.onSeek(500L)
        synchronizer.onSeek(4500L)

        // Ensure each seek invoked engine.stop()
        assertEquals(stopBefore + 5, engine.stopCount)
        assertEquals(0, engine.playedEvents.size)

        // Resuming from 4500ms should only play 5000ms and 8000ms
        synchronizer.onTimelineUpdate(5010L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals(5000L, engine.playedEvents[0].startTimeMs)
    }

    @Test
    fun pauseAndResume_controlsEngine() {
        val initialStops = engine.stopCount
        synchronizer.onPause()
        assertEquals(initialStops + 1, engine.stopCount)

        val initialStarts = engine.startCount
        synchronizer.onResume(2000L)
        assertEquals(initialStarts + 1, engine.startCount)
    }

    @Test
    fun playbackComplete_stopsEngine() {
        val initialStops = engine.stopCount
        synchronizer.onPlaybackComplete()
        assertEquals(initialStops + 1, engine.stopCount)
    }

    @Test
    fun disabledHaptics_suppressesActuation() {
        synchronizer.isHapticsEnabled = false
        synchronizer.onTimelineUpdate(1020L)
        synchronizer.onTimelineUpdate(2520L)
        assertEquals(0, engine.playedEvents.size)

        synchronizer.isHapticsEnabled = true
        synchronizer.onTimelineUpdate(5020L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals(5000L, engine.playedEvents[0].startTimeMs)
    }

    /**
     * Mandatory deterministic unit test verifying exact temporal synchronization:
     * - Event A: start=7100, end=11200
     * - Event B: start=27000, end=27800
     * - Event C: start=55100, end=58800
     * - Event D: start=67100, end=70500
     * - Event E: start=99100, end=99360
     *
     * Exact test positions:
     * 0, 5000, 7099, 7100, 7500, 11199, 11200, 15000, 26999, 27000, 27500, 27800,
     * 55100, 57000, 58800, 99100, 99200, 99359, 99360, 120000
     */
    @Test
    fun strictTemporalContainment_vibrationOnlyInsideIntervals() {
        val deterministicEvents = listOf(
            HapticEvent(id = "EventA", startTimeMs = 7100L, durationMs = 4100L, intensity = 0.8f),   // [7100..11200)
            HapticEvent(id = "EventB", startTimeMs = 27000L, durationMs = 800L, intensity = 0.9f),   // [27000..27800)
            HapticEvent(id = "EventC", startTimeMs = 55100L, durationMs = 3700L, intensity = 0.7f),  // [55100..58800)
            HapticEvent(id = "EventD", startTimeMs = 67100L, durationMs = 3400L, intensity = 0.85f), // [67100..70500)
            HapticEvent(id = "EventE", startTimeMs = 99100L, durationMs = 260L, intensity = 1.0f)    // [99100..99360)
        )
        val deterministicPattern = HapticPattern(videoId = "mandatory_test", events = deterministicEvents)
        val testEngine = RecordingHapticEngine()
        val testSynchronizer = HapticSynchronizer(engine = testEngine, toleranceMs = 50L)
        testSynchronizer.setPattern(deterministicPattern)

        data class CheckPoint(val positionMs: Long, val expectedVibrating: Boolean)

        val checkPoints = listOf(
            CheckPoint(0L, expectedVibrating = false),
            CheckPoint(5000L, expectedVibrating = false),
            CheckPoint(7099L, expectedVibrating = false),
            CheckPoint(7100L, expectedVibrating = true),   // Event A triggered
            CheckPoint(7500L, expectedVibrating = true),   // Event A active, no retrigger
            CheckPoint(11199L, expectedVibrating = true),  // Event A still active
            CheckPoint(11200L, expectedVibrating = false), // Event A ended, silent
            CheckPoint(15000L, expectedVibrating = false), // Silent gap
            CheckPoint(26999L, expectedVibrating = false), // Silent gap
            CheckPoint(27000L, expectedVibrating = true),  // Event B triggered
            CheckPoint(27500L, expectedVibrating = true),  // Event B active, no retrigger
            CheckPoint(27800L, expectedVibrating = false), // Event B ended, silent
            CheckPoint(55100L, expectedVibrating = true),  // Event C triggered
            CheckPoint(57000L, expectedVibrating = true),  // Event C active, no retrigger
            CheckPoint(58800L, expectedVibrating = false), // Event C ended, silent
            CheckPoint(99100L, expectedVibrating = true),  // Event E triggered
            CheckPoint(99200L, expectedVibrating = true),  // Event E active, no retrigger
            CheckPoint(99359L, expectedVibrating = true),  // Event E still active
            CheckPoint(99360L, expectedVibrating = false), // Event E ended, silent
            CheckPoint(120000L, expectedVibrating = false) // After final event, silent
        )

        for (cp in checkPoints) {
            testSynchronizer.onTimelineUpdate(cp.positionMs)
            assertEquals(
                "Vibration state mismatch at position ${cp.positionMs}ms",
                cp.expectedVibrating,
                testEngine.isVibrating
            )
            assertEquals(
                "Actuating state mismatch on synchronizer at position ${cp.positionMs}ms",
                cp.expectedVibrating,
                testSynchronizer.isActuatingState
            )
        }

        // Test seeking behavior
        // 1. Seek to 80000ms: gap between D and E -> silent
        testSynchronizer.onSeek(80000L)
        testSynchronizer.onTimelineUpdate(80000L)
        assertFalse("Position 80000ms must be silent", testEngine.isVibrating)

        // 2. Backward seek from 105000ms to 99100ms: collision event becomes eligible again
        testSynchronizer.onSeek(105000L)
        testSynchronizer.onTimelineUpdate(105000L)
        assertFalse("Position 105000ms must be silent", testEngine.isVibrating)

        testSynchronizer.onSeek(99100L)
        val countBeforeReentry = testEngine.playedEvents.size
        testSynchronizer.onTimelineUpdate(99100L)
        assertTrue("Collision must trigger once upon reentry at 99100ms", testEngine.isVibrating)
        assertEquals("Collision event must trigger exactly once", countBeforeReentry + 1, testEngine.playedEvents.size)

        testSynchronizer.onTimelineUpdate(99200L)
        assertTrue("Collision remains active at 99200ms without retriggering", testEngine.isVibrating)
        assertEquals("Collision must not retrigger at 99200ms", countBeforeReentry + 1, testEngine.playedEvents.size)

        testSynchronizer.onTimelineUpdate(99360L)
        assertFalse("Collision must stop exactly at 99360ms", testEngine.isVibrating)
    }
}
