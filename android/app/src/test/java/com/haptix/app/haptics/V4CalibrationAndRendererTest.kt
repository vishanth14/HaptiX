package com.haptix.app.haptics

import android.os.TestVibrator
import com.haptix.app.haptics.calibration.CalibrationManager
import com.haptix.app.haptics.calibration.CalibrationStimulus
import com.haptix.app.haptics.calibration.CalibrationTestResult
import com.haptix.app.haptics.calibration.DeviceHapticProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V4CalibrationAndRendererTest {

    @Test
    fun calibrationStimuli_has12SpecifiedStimuli() {
        val stimuli = CalibrationStimulus.ALL_STIMULI
        assertEquals(12, stimuli.size)

        // T01 to T05 are 100ms transients
        val expectedTransients = listOf(0.30f, 0.45f, 0.60f, 0.75f, 0.90f)
        for (i in expectedTransients.indices) {
            val stim = stimuli[i]
            assertEquals(String.format("T%02d", i + 1), stim.testId)
            assertEquals(100L, stim.durationMs)
            assertEquals(expectedTransients[i], stim.intensity, 0.001f)
            assertFalse(stim.isContinuous)
        }

        // T06 is 200ms transient
        val t06 = stimuli[5]
        assertEquals("T06", t06.testId)
        assertEquals(200L, t06.durationMs)
        assertEquals(0.60f, t06.intensity, 0.001f)
        assertFalse(t06.isContinuous)

        // T07-T09 are 400ms continuous
        val expected400 = listOf(0.40f, 0.60f, 0.80f)
        for (i in expected400.indices) {
            val stim = stimuli[6 + i]
            assertEquals(400L, stim.durationMs)
            assertEquals(expected400[i], stim.intensity, 0.001f)
            assertTrue(stim.isContinuous)
        }

        // T10-T12 are 1000ms continuous
        val expected1000 = listOf(0.50f, 0.70f, 0.90f)
        for (i in expected1000.indices) {
            val stim = stimuli[9 + i]
            assertEquals(1000L, stim.durationMs)
            assertEquals(expected1000[i], stim.intensity, 0.001f)
            assertTrue(stim.isContinuous)
        }
    }

    @Test
    fun deviceHapticProfile_defaultValues_areConservative() {
        val defaultProfile = DeviceHapticProfile.createDefault("Test Model", 34)
        assertEquals("Test Model", defaultProfile.deviceModel)
        assertEquals(34, defaultProfile.androidApi)
        assertTrue(defaultProfile.isDefault)
        assertEquals(0.35f, defaultProfile.minPerceivableIntensity, 0.001f)
        assertEquals(0.60f, defaultProfile.comfortableContinuousIntensity, 0.001f)
        assertEquals(0.82f, defaultProfile.strongEventIntensity, 0.001f)
        assertEquals(0.95f, defaultProfile.maxPracticalIntensity, 0.001f)
    }

    @Test
    fun calibrationManager_navigationAndRatingFlow() {
        val mockVibrator = TestVibrator(true, true, false, 205.0f)
        val caps = AndroidHapticCapabilities(customVibrator = mockVibrator)
        val player = AndroidHapticPlayer(capabilities = caps)
        val manager = CalibrationManager(hapticPlayer = player)

        // Initial index is 0 (T01)
        assertEquals(0, manager.currentIndex)
        assertEquals("T01", manager.currentStimulus.testId)

        // Next moves to T02
        manager.next()
        assertEquals(1, manager.currentIndex)
        assertEquals("T02", manager.currentStimulus.testId)

        // Record rating for T02
        manager.recordRating("T02", 4, true)
        val t02Result = manager.results["T02"]
        assertNotNull(t02Result)
        assertEquals(4, t02Result?.perceivedRating)
        assertEquals(true, t02Result?.isSmooth)

        // Previous moves back to T01
        manager.previous()
        assertEquals(0, manager.currentIndex)
        assertEquals("T01", manager.currentStimulus.testId)

        // Play stimulus does not throw
        manager.playCurrent()
        assertNotNull(player.lastDispatchedEvent)
        assertEquals(100L, player.lastDispatchedEvent?.durationMs)
    }

    @Test
    fun calibrationManager_computesCalibratedProfile() {
        val mockVibrator = TestVibrator(true, true, false, 205.0f)
        val caps = AndroidHapticCapabilities(customVibrator = mockVibrator)
        val player = AndroidHapticPlayer(capabilities = caps)
        val manager = CalibrationManager(hapticPlayer = player)

        // Simulate ratings: T01 (0.30) felt barely (2), T02 (0.45) felt well (3), T03 (0.60) felt clearly (4)
        manager.results["T01"] = CalibrationTestResult("T01", 180.0f, 0.30f, 100L, 2, true)
        manager.results["T02"] = CalibrationTestResult("T02", 180.0f, 0.45f, 100L, 3, true)
        manager.results["T03"] = CalibrationTestResult("T03", 200.0f, 0.60f, 100L, 4, true)
        manager.results["T04"] = CalibrationTestResult("T04", 210.0f, 0.75f, 100L, 5, true)

        val profile = manager.computeProfile("Calibrated Device", 34)
        assertFalse(profile.isDefault)
        // Perceptual floor should be derived from lowest rated >= 3 (which is 0.45f)
        assertEquals(0.45f, profile.minPerceivableIntensity, 0.01f)
    }

    @Test
    fun deviceHapticProfile_strongDefault_hasElevatedPerceptualFloor() {
        val strongProfile = DeviceHapticProfile.getStrongDefault("Strong Profile", 34)
        assertEquals("Strong Profile", strongProfile.deviceModel)
        assertEquals(34, strongProfile.androidApi)
        assertTrue(strongProfile.isCalibrated)
        assertEquals(0.50f, strongProfile.minPerceivableIntensity, 0.001f)
        assertEquals(0.72f, strongProfile.comfortableContinuousIntensity, 0.001f)
        assertEquals(0.88f, strongProfile.strongEventIntensity, 0.001f)
        assertEquals(1.00f, strongProfile.maxPracticalIntensity, 0.001f)
    }
}

