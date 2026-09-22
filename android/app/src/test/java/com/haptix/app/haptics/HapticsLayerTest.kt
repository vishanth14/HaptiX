package com.haptix.app.haptics

import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticEventType
import com.haptix.app.data.model.HapticPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticsLayerTest {

    // 7. Basic haptic event timing representation test
    @Test
    fun hapticEventTiming_computesCorrectTimelineBoundaries() {
        val transientEvent = HapticEvent(
            type = HapticEventType.TRANSIENT,
            startTimeMs = 2500L,
            durationMs = 40L,
            intensity = 0.9f,
            sharpness = 0.8f
        )

        assertEquals(2500L, transientEvent.startTimeMs)
        assertEquals(40L, transientEvent.durationMs)
        assertEquals(2540L, transientEvent.endTimeMs)

        val continuousEvent = HapticEvent(
            type = HapticEventType.CONTINUOUS,
            startTimeMs = 5000L,
            durationMs = 1500L,
            intensity = 0.7f,
            sharpness = 0.3f
        )

        assertEquals(5000L, continuousEvent.startTimeMs)
        assertEquals(1500L, continuousEvent.durationMs)
        assertEquals(6500L, continuousEvent.endTimeMs)
    }

    @Test
    fun hapticEventTiming_sortsChronologically() {
        val eventEarly = HapticEvent(type = HapticEventType.TRANSIENT, startTimeMs = 100L)
        val eventMid = HapticEvent(type = HapticEventType.CONTINUOUS, startTimeMs = 500L, durationMs = 200L)
        val eventLate = HapticEvent(type = HapticEventType.TRANSIENT, startTimeMs = 1200L)

        val pattern = HapticPattern(
            videoId = "vid_timing_test",
            events = listOf(eventLate, eventEarly, eventMid) // Deliberately unsorted
        )

        val sorted = pattern.sortedEvents()
        assertEquals(100L, sorted[0].startTimeMs)
        assertEquals(500L, sorted[1].startTimeMs)
        assertEquals(1200L, sorted[2].startTimeMs)
    }

    // 8. Basic haptic pattern creation test
    @Test
    fun hapticPatternCreation_assemblesAndCalculatesSpan() {
        val events = listOf(
            HapticEvent(type = HapticEventType.TRANSIENT, startTimeMs = 0L, durationMs = 50L),
            HapticEvent(type = HapticEventType.CONTINUOUS, startTimeMs = 1000L, durationMs = 400L),
            HapticEvent(type = HapticEventType.TRANSIENT, startTimeMs = 2500L, durationMs = 80L)
        )

        val pattern = HapticPattern(
            videoId = "clip_modulation",
            version = "1.0",
            events = events
        )

        assertEquals("clip_modulation", pattern.videoId)
        assertEquals(3, pattern.eventCount)
        assertEquals(2580L, pattern.totalDurationMs) // 2500 + 80 = 2580
    }

    @Test
    fun hapticPatternCreation_emptyPatternHandledSafely() {
        val emptyPattern = HapticPattern(videoId = "empty_clip")
        assertEquals(0, emptyPattern.eventCount)
        assertEquals(0L, emptyPattern.totalDurationMs)
        assertTrue(emptyPattern.events.isEmpty())
    }

    // Synchronizer timing coordination test
    @Test
    fun hapticSynchronizer_identifiesEventsWithinTimelineWindow() {
        val playedEvents = mutableListOf<HapticEvent>()
        val mockEngine = object : HapticEngine {
            var isPlaying = false
            override fun isHapticSupported(): Boolean = true
            override fun loadPattern(pattern: HapticPattern) {}
            override fun start() { isPlaying = true }
            override fun stop() { isPlaying = false }
            override fun release() { isPlaying = false }
            override fun playEvent(event: HapticEvent) {
                playedEvents.add(event)
            }
        }

        val synchronizer = HapticSynchronizer(engine = mockEngine, toleranceMs = 50L)
        val event1 = HapticEvent(type = HapticEventType.TRANSIENT, startTimeMs = 1000L, durationMs = 50L)
        val event2 = HapticEvent(type = HapticEventType.CONTINUOUS, startTimeMs = 3000L, durationMs = 200L)

        synchronizer.setPattern(HapticPattern(videoId = "vid_sync", events = listOf(event1, event2)))

        // Timeline position before event
        synchronizer.onTimelineUpdate(500L)
        assertEquals(0, playedEvents.size)

        // Timeline position within event1 window (1000ms .. 1050ms)
        synchronizer.onTimelineUpdate(1020L)
        assertEquals(1, playedEvents.size)
        assertEquals(1000L, playedEvents[0].startTimeMs)

        // Toggle haptics disabled
        synchronizer.isHapticsEnabled = false
        synchronizer.onTimelineUpdate(3010L)
        assertEquals(1, playedEvents.size) // No additional event triggered
    }
}
