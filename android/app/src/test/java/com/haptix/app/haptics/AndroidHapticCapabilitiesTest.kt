package com.haptix.app.haptics

import android.os.TestVibrator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidHapticCapabilitiesTest {

    @Test
    fun noVibrator_reportsUnavailable() {
        val caps = AndroidHapticCapabilities(customVibrator = null)
        val profile = caps.getHardwareProfile()

        assertEquals(HapticCapabilityLevel.UNAVAILABLE, profile.capabilityLevel)
        assertFalse(profile.hasVibrator)
        assertFalse(profile.areEnvelopeEffectsSupported)
        assertEquals("HAPTICS UNAVAILABLE", profile.statusLabel)
    }

    @Test
    fun vibratorPresentFalse_reportsUnavailable() {
        val fakeVib = TestVibrator(false, true, false, 205.0f)
        val caps = AndroidHapticCapabilities(customVibrator = fakeVib)
        val profile = caps.getHardwareProfile()

        assertEquals(HapticCapabilityLevel.UNAVAILABLE, profile.capabilityLevel)
        assertFalse(profile.hasVibrator)
    }

    @Test
    fun standardActuator_reportsLimited() {
        val fakeVib = TestVibrator(true, true, false, 205.0f)
        val caps = AndroidHapticCapabilities(customVibrator = fakeVib)
        val profile = caps.getHardwareProfile()

        assertEquals(HapticCapabilityLevel.LIMITED, profile.capabilityLevel)
        assertTrue(profile.hasVibrator)
        assertTrue(profile.hasAmplitudeControl)
        assertFalse(profile.areEnvelopeEffectsSupported)
        assertEquals("HAPTICS LIMITED", profile.statusLabel)
        assertEquals(205.0f, profile.resonantFrequencyHz)
    }

    @Test
    fun advancedActuator_reportsSupported() {
        val fakeVib = TestVibrator(true, true, true, 205.0f)
        val caps = AndroidHapticCapabilities(customVibrator = fakeVib)
        val profile = caps.getHardwareProfile()

        assertEquals(HapticCapabilityLevel.SUPPORTED, profile.capabilityLevel)
        assertTrue(profile.hasVibrator)
        assertTrue(profile.areEnvelopeEffectsSupported)
        assertEquals("HAPTICS READY", profile.statusLabel)
    }

    @Test
    fun invalidResonantFrequency_reportsNull() {
        val fakeVib = TestVibrator(true, true, false, Float.NaN)
        val caps = AndroidHapticCapabilities(customVibrator = fakeVib)
        val profile = caps.getHardwareProfile()

        assertNull(profile.resonantFrequencyHz)
    }
}
