package com.haptix.app.haptics

import android.os.TestVibrator
import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticEventType
import com.haptix.app.data.model.HapticFrequencyPoint
import com.haptix.app.data.model.HapticPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidHapticPlayerTest {

    @Test
    fun player_withSupportedHardware_reflectsCapability() {
        val mockVib = TestVibrator(true, true, false, 205.0f)
        val caps = AndroidHapticCapabilities(customVibrator = mockVib)
        val player = AndroidHapticPlayer(capabilities = caps)

        assertTrue(player.isHapticSupported())
        assertEquals(HapticCapabilityLevel.LIMITED, player.getCapabilityLevel())
    }

    @Test
    fun player_withUnavailableHardware_reflectsUnavailable() {
        val mockVib = TestVibrator(false, false, false, Float.NaN)
        val caps = AndroidHapticCapabilities(customVibrator = mockVib)
        val player = AndroidHapticPlayer(capabilities = caps)

        assertFalse(player.isHapticSupported())
        assertEquals(HapticCapabilityLevel.UNAVAILABLE, player.getCapabilityLevel())

        // Play event does not crash or dispatch
        val event = HapticEvent(startTimeMs = 0L, durationMs = 50L)
        player.playEvent(event)
        assertNull(player.lastDispatchedEvent)
    }

    @Test
    fun player_playEvent_recordsDebugTelemetry() {
        val mockVib = TestVibrator(true, true, false, 205.0f)
        val caps = AndroidHapticCapabilities(customVibrator = mockVib)
        val player = AndroidHapticPlayer(capabilities = caps)

        val event = HapticEvent(
            type = HapticEventType.TRANSIENT,
            startTimeMs = 1200L,
            durationMs = 80L,
            intensity = 0.75f,
            sharpness = 0.5f,
            parameters = mapOf("frequencyHz" to 220.0f, "amplitude" to 0.75f)
        )

        player.playEvent(event)
        assertEquals(event, player.lastDispatchedEvent)
        assertEquals(220.0f, player.lastDispatchedFrequencyHz)
        assertEquals(0.75f, player.lastDispatchedAmplitude)
    }

    @Test
    fun player_playFrequencyPoint_delegatesToEvent() {
        val mockVib = TestVibrator(true, true, false, 205.0f)
        val caps = AndroidHapticCapabilities(customVibrator = mockVib)
        val player = AndroidHapticPlayer(capabilities = caps)

        val point = HapticFrequencyPoint(
            startTimeMs = 2500L,
            durationMs = 150L,
            frequencyHz = 235.0f,
            amplitude = 0.9f
        )

        player.playFrequencyPoint(point)
        assertNotNull(player.lastDispatchedEvent)
        assertEquals(235.0f, player.lastDispatchedFrequencyHz)
        assertEquals(0.9f, player.lastDispatchedAmplitude)
    }

    @Test
    fun player_stopAndRelease_cancelsVibrator() {
        val mockVib = TestVibrator(true, true, false, 205.0f)
        val caps = AndroidHapticCapabilities(customVibrator = mockVib)
        val player = AndroidHapticPlayer(capabilities = caps)

        player.loadPattern(HapticPattern(videoId = "test_vid"))
        player.start()
        assertTrue(player.isPlaying)

        player.stop()
        assertFalse(player.isPlaying)
        assertEquals(1, mockVib.cancelCalls)

        player.release()
        assertFalse(player.isPlaying)
        assertNull(player.lastDispatchedEvent)
    }
}
