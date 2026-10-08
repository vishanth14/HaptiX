package com.haptix.app.haptics

import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Dedicated unit test suite verifying the 12 core synchronization requirements:
 *
 * TEST 1: Start video at 0 ms.
 * TEST 2: Play -> pause during a haptic event.
 * TEST 3: Play -> pause -> wait several seconds -> resume (authoritative video position, not wall-clock).
 * TEST 4: Play -> seek forward across multiple haptic events.
 * TEST 5: Play -> seek backward (re-arms previously passed events).
 * TEST 6: Rapid seeking (20s -> 50s -> 10s -> 75s -> 30s).
 * TEST 7: Pause -> seek -> resume (haptics correspond to the new position).
 * TEST 8: Turn Haptics OFF while playing.
 * TEST 9: Turn Haptics ON while playing (synchronize from current position).
 * TEST 10: Video reaches end (all haptics stop, pending work cancelled).
 * TEST 11: Replay from beginning (all haptics eligible again).
 * TEST 12: Buffering/interruption (haptics never run ahead of video clock).
 */
class VideoHapticAuthoritativeSyncTest {

    private class RecordingHapticEngine : HapticEngine {
        val playedEvents = mutableListOf<HapticEvent>()
        var startCount = 0
        var stopCount = 0
        var isVibrating = false
        val stopReasons = mutableListOf<String>()

        override fun isHapticSupported(): Boolean = true
        override fun loadPattern(pattern: HapticPattern) {}
        override fun start() {
            startCount++
        }
        override fun stop() {
            stopCount++
            isVibrating = false
            stopReasons.add("UNSPECIFIED")
        }
        override fun stop(reason: String) {
            stopCount++
            isVibrating = false
            stopReasons.add(reason)
        }
        override fun release() {
            stop()
        }
        override fun playEvent(event: HapticEvent) {
            playedEvents.add(event)
            isVibrating = true
        }

        fun resetRecording() {
            playedEvents.clear()
            startCount = 0
            stopCount = 0
            isVibrating = false
            stopReasons.clear()
        }
    }

    private lateinit var engine: RecordingHapticEngine
    private lateinit var synchronizer: HapticSynchronizer

    @Before
    fun setUp() {
        engine = RecordingHapticEngine()
        synchronizer = HapticSynchronizer(engine = engine, minimumImpulseDurationMs = 50L)
    }

    // ========================================================================
    // TEST 1: Start video at 0. Verify haptics begin at the correct timestamps.
    // ========================================================================
    @Test
    fun test01_startAtZero_hapticsBeginAtExactTimestamps() {
        val pattern = HapticPattern(
            videoId = "test_01",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 200L, intensity = 0.6f),
                HapticEvent(id = "E2", startTimeMs = 2500L, durationMs = 300L, intensity = 0.8f)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Silent interval before 1000ms
        synchronizer.onTimelineUpdate(0L)
        synchronizer.onTimelineUpdate(500L)
        synchronizer.onTimelineUpdate(950L)
        assertEquals(0, engine.playedEvents.size)
        assertFalse(engine.isVibrating)

        // Enters onset at 1000ms
        synchronizer.onTimelineUpdate(1000L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("E1", engine.playedEvents[0].id)
        assertTrue(engine.isVibrating)

        // Mid-event tick does not re-trigger
        synchronizer.onTimelineUpdate(1100L)
        assertEquals(1, engine.playedEvents.size)
        assertTrue(engine.isVibrating)

        // Silence interval between 1200ms and 2500ms
        synchronizer.onTimelineUpdate(1250L)
        assertFalse(engine.isVibrating)

        // Enters E2 at 2500ms
        synchronizer.onTimelineUpdate(2500L)
        assertEquals(2, engine.playedEvents.size)
        assertEquals("E2", engine.playedEvents[1].id)
        assertTrue(engine.isVibrating)
    }

    // ========================================================================
    // TEST 2: Play -> pause during a haptic event. Verify vibration stops immediately.
    // ========================================================================
    @Test
    fun test02_playPauseDuringEvent_vibrationStopsImmediately() {
        val pattern = HapticPattern(
            videoId = "test_02",
            events = listOf(
                HapticEvent(id = "LONG_EVENT", startTimeMs = 1000L, durationMs = 3000L, intensity = 0.9f)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Step into active interval [1000..4000)
        synchronizer.onTimelineUpdate(1500L)
        assertTrue("Actuator must be actively vibrating", engine.isVibrating)
        assertTrue("Synchronizer must report active actuation", synchronizer.isActuatingState)

        // User pauses video at 1500ms
        val initialStops = engine.stopCount
        synchronizer.onPause("USER_PAUSE", positionMs = 1500L)

        assertFalse("Vibration must stop immediately upon pause", engine.isVibrating)
        assertFalse("Synchronizer must report silence", synchronizer.isActuatingState)
        assertEquals(PlaybackSyncState.PAUSED, synchronizer.syncState)
        assertEquals(initialStops + 1, engine.stopCount)
    }

    // ========================================================================
    // TEST 3: Play -> pause -> wait several seconds -> resume.
    // Verify haptics resume from the video position, NOT wall-clock time.
    // ========================================================================
    @Test
    fun test03_pauseWaitResume_resumesFromVideoPositionNotWallClock() {
        val pattern = HapticPattern(
            videoId = "test_03",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 2000L), // [1000..3000)
                HapticEvent(id = "E2", startTimeMs = 5000L, durationMs = 1000L)  // [5000..6000)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Play up to 1500ms
        synchronizer.onTimelineUpdate(1500L)
        assertEquals(1, engine.playedEvents.size)

        // Pause at 1500ms
        synchronizer.onPause("PAUSE", positionMs = 1500L)
        assertFalse(engine.isVibrating)

        // Wall-clock time advances by 10 seconds in the real world,
        // but video position is still 1500ms.
        // User resumes playback at 1500ms:
        synchronizer.onResume(1500L)
        assertEquals(PlaybackSyncState.PLAYING, synchronizer.syncState)

        // On timeline update at 1500ms, in-progress event E1 re-actuates
        synchronizer.onTimelineUpdate(1500L)
        assertTrue("E1 must resume actuation at video timestamp 1500ms", engine.isVibrating)
        assertEquals(2, engine.playedEvents.size) // E1 re-actuated for resumed pass

        // Progress past 3000ms: E1 ends, silence before E2
        synchronizer.onTimelineUpdate(3100L)
        assertFalse(engine.isVibrating)

        // At 5000ms: E2 triggers correctly according to video timeline
        synchronizer.onTimelineUpdate(5000L)
        assertEquals(3, engine.playedEvents.size)
        assertEquals("E2", engine.playedEvents.last().id)
    }

    // ========================================================================
    // TEST 4: Play -> seek forward across multiple haptic events.
    // Verify no old events fire after the seek.
    // ========================================================================
    @Test
    fun test04_seekForwardAcrossEvents_noSkippedEventsFire() {
        val pattern = HapticPattern(
            videoId = "test_04",
            events = listOf(
                HapticEvent(id = "E_10s", startTimeMs = 10000L, durationMs = 500L),
                HapticEvent(id = "E_20s", startTimeMs = 20000L, durationMs = 500L),
                HapticEvent(id = "E_30s", startTimeMs = 30000L, durationMs = 500L),
                HapticEvent(id = "E_40s", startTimeMs = 40000L, durationMs = 500L),
                HapticEvent(id = "E_60s", startTimeMs = 60000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Play past E_10s (at 10200ms)
        synchronizer.onTimelineUpdate(10200L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("E_10s", engine.playedEvents[0].id)

        // Seek forward from 10.2s to 55.0s (skipping 20s, 30s, 40s)
        synchronizer.onSeek(55000L)
        assertEquals("Seek itself must never actuate", 1, engine.playedEvents.size)
        assertFalse(engine.isVibrating)

        // Timeline advances from 55s: skipped events (20s, 30s, 40s) must NOT fire
        synchronizer.onTimelineUpdate(55100L)
        synchronizer.onTimelineUpdate(59000L)
        assertEquals(1, engine.playedEvents.size)

        // At 60s, E_60s triggers
        synchronizer.onTimelineUpdate(60000L)
        assertEquals(2, engine.playedEvents.size)
        assertEquals("E_60s", engine.playedEvents[1].id)
    }

    // ========================================================================
    // TEST 5: Play -> seek backward.
    // Verify previously passed events become eligible again.
    // ========================================================================
    @Test
    fun test05_seekBackward_restoresEventEligibility() {
        val pattern = HapticPattern(
            videoId = "test_05",
            events = listOf(
                HapticEvent(id = "E_35s", startTimeMs = 35000L, durationMs = 500L),
                HapticEvent(id = "E_70s", startTimeMs = 70000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Play E_35s and progress to 80s
        synchronizer.onTimelineUpdate(35100L)
        assertEquals(1, engine.playedEvents.size)
        synchronizer.onTimelineUpdate(70100L)
        assertEquals(2, engine.playedEvents.size)
        synchronizer.onTimelineUpdate(80000L)
        assertFalse(engine.isVibrating)

        // Seek backward: 80s -> 30s
        synchronizer.onSeek(30000L)
        assertFalse(engine.isVibrating)

        // Play forward from 30s: E_35s must play again on this pass
        synchronizer.onTimelineUpdate(35100L)
        assertEquals(3, engine.playedEvents.size)
        assertEquals("E_35s", engine.playedEvents[2].id)
    }

    // ========================================================================
    // TEST 6: Rapid seeking: 20 -> 50 -> 10 -> 75 -> 30 sec.
    // Verify only the final playback position controls haptics.
    // ========================================================================
    @Test
    fun test06_rapidSeeking_onlyFinalTargetControlsHaptics() {
        val pattern = HapticPattern(
            videoId = "test_06",
            events = listOf(
                HapticEvent(id = "E_15s", startTimeMs = 15000L, durationMs = 500L),
                HapticEvent(id = "E_25s", startTimeMs = 25000L, durationMs = 500L),
                HapticEvent(id = "E_35s", startTimeMs = 35000L, durationMs = 500L),
                HapticEvent(id = "E_55s", startTimeMs = 55000L, durationMs = 500L),
                HapticEvent(id = "E_80s", startTimeMs = 80000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Rapid seeks across timeline
        val initialGeneration = synchronizer.playbackGeneration
        synchronizer.onSeek(20000L)
        synchronizer.onSeek(50000L)
        synchronizer.onSeek(10000L)
        synchronizer.onSeek(75000L)
        synchronizer.onSeek(30000L)

        // Every seek must have incremented generation token
        assertEquals(initialGeneration + 5, synchronizer.playbackGeneration)
        assertEquals(0, engine.playedEvents.size)
        assertFalse(engine.isVibrating)

        // Playback settles at 30s and progresses forward
        synchronizer.onTimelineUpdate(30000L)
        assertEquals(0, engine.playedEvents.size) // No event at 30s

        // Event at 35s must be the FIRST event to fire
        synchronizer.onTimelineUpdate(35000L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("E_35s", engine.playedEvents[0].id)
    }

    // ========================================================================
    // TEST 7: Pause -> seek -> resume.
    // Verify haptics correspond to the NEW position.
    // ========================================================================
    @Test
    fun test07_pauseSeekResume_hapticsCorrespondToNewPosition() {
        val pattern = HapticPattern(
            videoId = "test_07",
            events = listOf(
                HapticEvent(id = "E_OLD", startTimeMs = 10000L, durationMs = 1000L),
                HapticEvent(id = "E_NEW", startTimeMs = 42000L, durationMs = 1000L) // [42000..43000)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Play old event
        synchronizer.onTimelineUpdate(10200L)
        assertEquals(1, engine.playedEvents.size)

        // 1. Pause at 10.5s
        synchronizer.onPause("PAUSE", positionMs = 10500L)
        assertFalse(engine.isVibrating)

        // 2. Seek to 42.5s (directly inside E_NEW)
        synchronizer.onSeek(42500L)
        assertFalse(engine.isVibrating)

        // 3. Resume at 42.5s
        synchronizer.onResume(42500L)

        // 4. Authoritative update at 42.5s triggers E_NEW
        synchronizer.onTimelineUpdate(42500L)
        assertEquals(2, engine.playedEvents.size)
        assertEquals("E_NEW", engine.playedEvents[1].id)
        assertTrue(engine.isVibrating)

        // E_NEW finishes at 43.0s
        synchronizer.onTimelineUpdate(43000L)
        assertFalse(engine.isVibrating)
    }

    // ========================================================================
    // TEST 8: Turn Haptics OFF while playing. Verify vibration stops immediately.
    // ========================================================================
    @Test
    fun test08_turnHapticsOffWhilePlaying_vibrationStopsImmediately() {
        val pattern = HapticPattern(
            videoId = "test_08",
            events = listOf(
                HapticEvent(id = "E_ACTIVE", startTimeMs = 1000L, durationMs = 5000L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Actuate event
        synchronizer.onTimelineUpdate(2000L)
        assertTrue(engine.isVibrating)
        assertTrue(synchronizer.isActuatingState)

        // User turns haptics OFF
        val initialStops = engine.stopCount
        synchronizer.isHapticsEnabled = false

        assertFalse("Vibration must immediately cease", engine.isVibrating)
        assertFalse("Synchronizer must report silence", synchronizer.isActuatingState)
        assertEquals(initialStops + 1, engine.stopCount)

        // Subsequent timeline updates remain silent
        synchronizer.onTimelineUpdate(2500L)
        synchronizer.onTimelineUpdate(3000L)
        assertFalse(engine.isVibrating)
    }

    // ========================================================================
    // TEST 9: Turn Haptics ON while playing.
    // Verify haptics synchronize from the CURRENT video position.
    // ========================================================================
    @Test
    fun test09_turnHapticsOnWhilePlaying_synchronizesFromCurrentPosition() {
        val pattern = HapticPattern(
            videoId = "test_09",
            events = listOf(
                HapticEvent(id = "E_PAST", startTimeMs = 10000L, durationMs = 1000L),
                HapticEvent(id = "E_65s", startTimeMs = 65000L, durationMs = 1000L),
                HapticEvent(id = "E_FUTURE", startTimeMs = 80000L, durationMs = 1000L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.isHapticsEnabled = false
        synchronizer.onResume(0L)

        // Video plays with haptics disabled up to 64,500ms
        synchronizer.onTimelineUpdate(10000L)
        synchronizer.onTimelineUpdate(64500L)
        assertEquals(0, engine.playedEvents.size)

        // User turns haptics ON at 64,500ms
        synchronizer.isHapticsEnabled = true

        // E_PAST (10s) must NOT fire.
        // At 65s, E_65s triggers:
        synchronizer.onTimelineUpdate(65000L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("E_65s", engine.playedEvents[0].id)
        assertTrue(engine.isVibrating)
    }

    // ========================================================================
    // TEST 10: Allow video to reach the end.
    // Verify no haptic occurs after video completion.
    // ========================================================================
    @Test
    fun test10_videoEnd_stopsHapticsAndCancelsPendingWork() {
        val pattern = HapticPattern(
            videoId = "test_10",
            videoDurationMs = 10000L,
            events = listOf(
                HapticEvent(id = "E_FINAL", startTimeMs = 8000L, durationMs = 2000L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        synchronizer.onTimelineUpdate(8500L)
        assertTrue(engine.isVibrating)

        // Video completes at 10,000ms
        synchronizer.onPlaybackComplete()
        assertFalse(engine.isVibrating)
        assertFalse(synchronizer.isActuatingState)
        assertEquals(PlaybackSyncState.STOPPED, synchronizer.syncState)

        // Spurious ticks after end are rejected
        synchronizer.onTimelineUpdate(10100L)
        assertFalse(engine.isVibrating)
    }

    // ========================================================================
    // TEST 11: Replay from beginning. Verify haptic events can play again.
    // ========================================================================
    @Test
    fun test11_replayFromBeginning_resetsTimelineCorrectly() {
        val pattern = HapticPattern(
            videoId = "test_11",
            events = listOf(
                HapticEvent(id = "E1", startTimeMs = 1000L, durationMs = 300L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        // Pass 1: play through
        synchronizer.onTimelineUpdate(1050L)
        assertEquals(1, engine.playedEvents.size)
        synchronizer.onTimelineUpdate(2000L)
        synchronizer.onPlaybackComplete()

        // Replay: seek to 0 and resume
        synchronizer.onSeek(0L)
        synchronizer.onResume(0L)
        engine.playedEvents.clear()

        // Pass 2: E1 triggers again
        synchronizer.onTimelineUpdate(1050L)
        assertEquals(1, engine.playedEvents.size)
        assertEquals("E1", engine.playedEvents[0].id)
    }

    // ========================================================================
    // TEST 12: Buffering/interruption.
    // Verify haptics never run ahead of video playback.
    // ========================================================================
    @Test
    fun test12_buffering_hapticsNeverRunAheadOfVideoClock() {
        val pattern = HapticPattern(
            videoId = "test_12",
            events = listOf(
                HapticEvent(id = "E_ACTIVE", startTimeMs = 1000L, durationMs = 2000L),
                HapticEvent(id = "E_UPCOMING", startTimeMs = 4000L, durationMs = 500L)
            )
        )
        synchronizer.setPattern(pattern)
        synchronizer.onResume(0L)

        synchronizer.onTimelineUpdate(1500L)
        assertTrue(engine.isVibrating)

        // ExoPlayer enters BUFFERING at 1500ms
        synchronizer.onBuffering(positionMs = 1500L)
        assertFalse("Buffering must immediately silence actuator", engine.isVibrating)
        assertEquals(PlaybackSyncState.BUFFERING, synchronizer.syncState)

        // While buffering, ticks must not actuate upcoming events
        synchronizer.onTimelineUpdate(1500L)
        assertFalse(engine.isVibrating)

        // Playback resumes after buffering at 1500ms
        synchronizer.onResume(1500L)
        synchronizer.onTimelineUpdate(1500L)
        assertTrue("Active event resumes upon playback recovery", engine.isVibrating)

        // Progress to 4000ms: upcoming event fires in synchronization with video clock
        synchronizer.onTimelineUpdate(4000L)
        assertEquals(3, engine.playedEvents.size) // E_ACTIVE, E_ACTIVE (re-armed), E_UPCOMING
        assertEquals("E_UPCOMING", engine.playedEvents.last().id)
    }
}
