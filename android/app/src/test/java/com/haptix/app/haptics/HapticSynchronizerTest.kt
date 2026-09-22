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

        override fun isHapticSupported(): Boolean = true
        override fun loadPattern(pattern: HapticPattern) {}
        override fun start() { startCount++ }
        override fun stop() { stopCount++ }
        override fun release() { stop() }
        override fun playEvent(event: HapticEvent) {
            playedEvents.add(event)
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
}
