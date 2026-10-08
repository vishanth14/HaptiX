package com.haptix.app.haptics

import com.haptix.app.data.model.HapticEnvelope
import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.SemanticHapticType
import com.haptix.app.haptics.calibration.DeviceHapticCalibration
import com.haptix.app.haptics.calibration.DeviceHapticCalibrationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Step 15: Comprehensive unit tests validating [DeviceHapticCalibration]:
 * - Low amplitude (Quiet)
 * - Medium amplitude
 * - High amplitude (Strong)
 * - Crash dominance
 * - Climax progression (strict monotonic growth across Beats 1 to 6)
 * - Continuous rumble smoothness
 * - Transient pre-emphasis attack compensation
 * - Amplitude floor calibration (28 instead of 60)
 * - Separation of source intensity from device render amplitude
 */
class DeviceHapticCalibrationTest {

    private val calibration = DeviceHapticCalibration.DEFAULT

    @Test
    fun lowAmplitude_mapsToQuietPerceptibleLevel() {
        val event = HapticEvent(
            startTimeMs = 1000L,
            durationMs = 100L,
            intensity = 0.15f,
            semanticType = SemanticHapticType.SOFT_IMPACT
        )
        val profile = SemanticHapticPatternGenerator.generateWaveformProfile(event)
        val calibrated = calibration.calibrate(profile, event)

        // Quiet tier: source <= 0.20 -> calibrated peak between 30 and 65
        assertTrue("Quiet peak must be >= 28", calibrated.calibratedPeak >= 28)
        assertTrue("Quiet peak must be <= 65, was ${calibrated.calibratedPeak}", calibrated.calibratedPeak <= 65)
        assertEquals("Source intensity must be strictly preserved", 0.15f, calibrated.sourceIntensity, 0.001f)
    }

    @Test
    fun mediumAmplitude_mapsToDistinctNoticeableLevel() {
        val event = HapticEvent(
            startTimeMs = 26540L,
            durationMs = 260L,
            intensity = 0.455f,
            semanticType = SemanticHapticType.GEAR_SHIFT
        )
        val profile = SemanticHapticPatternGenerator.generateWaveformProfile(event)
        val calibrated = calibration.calibrate(profile, event)

        // Medium tier: source 0.35..0.55 -> calibrated peak 120..195
        assertTrue("Medium peak must be >= 120, was ${calibrated.calibratedPeak}", calibrated.calibratedPeak >= 120)
        assertTrue("Medium peak must be <= 210, was ${calibrated.calibratedPeak}", calibrated.calibratedPeak <= 210)
        assertEquals(0.455f, calibrated.sourceIntensity, 0.001f)
    }

    @Test
    fun strongAmplitude_mapsToFirmMechanicalLevel() {
        val event = HapticEvent(
            startTimeMs = 5000L,
            durationMs = 240L,
            intensity = 0.65f,
            semanticType = SemanticHapticType.HEAVY_IMPACT
        )
        val profile = SemanticHapticPatternGenerator.generateWaveformProfile(event)
        val calibrated = calibration.calibrate(profile, event)

        // Strong tier: source 0.55..0.70 -> calibrated peak 175..235
        assertTrue("Strong peak must be >= 175, was ${calibrated.calibratedPeak}", calibrated.calibratedPeak >= 175)
        assertTrue("Strong peak must be <= 240, was ${calibrated.calibratedPeak}", calibrated.calibratedPeak <= 240)
    }

    @Test
    fun crashLandmark_deliversMaximumActuatorSaturation() {
        val crashEvent = HapticEvent(
            startTimeMs = 105020L,
            durationMs = 180L,
            intensity = 0.760f,
            semanticType = SemanticHapticType.COLLISION,
            attackMs = 15L,
            sustainMs = 65L,
            releaseMs = 100L,
            parameters = mapOf("description" to "Landmark Crash Event")
        )
        val profile = SemanticHapticPatternGenerator.generateWaveformProfile(crashEvent)
        val calibrated = calibration.calibrate(profile, crashEvent)

        assertEquals("Crash peak must hit maximum 255 ceiling", 255, calibrated.calibratedPeak)
        assertTrue("Crash RMS must be high (>= 165.0), was ${calibrated.rms}", calibrated.rms >= 165.0)
        assertTrue("Crash area must be substantial (>= 22000), was ${calibrated.area}", calibrated.area >= 22000L)
        assertEquals("Source intensity must remain 0.760", 0.760f, calibrated.sourceIntensity, 0.001f)
    }

    @Test
    fun climaxBeats_exhibitStrictMonotonicGrowthAcrossAllSixBeats() {
        val climaxEvents = listOf(
            HapticEvent(startTimeMs = 123440L, durationMs = 240L, intensity = 0.435f, semanticType = SemanticHapticType.HEAVY_IMPACT, attackMs = 20L, sustainMs = 45L, releaseMs = 175L, parameters = mapOf("description" to "Climax Beat 1")),
            HapticEvent(startTimeMs = 123940L, durationMs = 220L, intensity = 0.529f, semanticType = SemanticHapticType.HEAVY_IMPACT, attackMs = 25L, sustainMs = 60L, releaseMs = 135L, parameters = mapOf("description" to "Climax Beat 2")),
            HapticEvent(startTimeMs = 124440L, durationMs = 220L, intensity = 0.567f, semanticType = SemanticHapticType.HEAVY_IMPACT, attackMs = 25L, sustainMs = 75L, releaseMs = 120L, parameters = mapOf("description" to "Climax Beat 3")),
            HapticEvent(startTimeMs = 124940L, durationMs = 220L, intensity = 0.592f, semanticType = SemanticHapticType.HEAVY_IMPACT, attackMs = 30L, sustainMs = 90L, releaseMs = 100L, parameters = mapOf("description" to "Climax Beat 4")),
            HapticEvent(startTimeMs = 125440L, durationMs = 240L, intensity = 0.641f, semanticType = SemanticHapticType.HEAVY_IMPACT, attackMs = 30L, sustainMs = 115L, releaseMs = 95L, parameters = mapOf("description" to "Climax Beat 5")),
            HapticEvent(startTimeMs = 125920L, durationMs = 300L, intensity = 0.670f, semanticType = SemanticHapticType.HEAVY_IMPACT, attackMs = 20L, sustainMs = 140L, releaseMs = 140L, parameters = mapOf("description" to "Climax Beat 6"))
        )

        val calibratedBeats = climaxEvents.map { ev ->
            val prof = SemanticHapticPatternGenerator.generateWaveformProfile(ev)
            calibration.calibrate(prof, ev)
        }

        // Verify strictly monotonic growth:
        for (i in 0 until calibratedBeats.size - 1) {
            val curr = calibratedBeats[i]
            val next = calibratedBeats[i + 1]

            assertTrue(
                "Peak amplitude must strictly increase from Beat ${i + 1} (${curr.calibratedPeak}) to Beat ${i + 2} (${next.calibratedPeak})",
                curr.calibratedPeak < next.calibratedPeak
            )
            assertTrue(
                "RMS energy must strictly increase from Beat ${i + 1} (${curr.rms}) to Beat ${i + 2} (${next.rms})",
                curr.rms < next.rms
            )
            assertTrue(
                "Total area must strictly increase from Beat ${i + 1} (${curr.area}) to Beat ${i + 2} (${next.area})",
                curr.area < next.area
            )
        }

        // Beat 6 must be substantially stronger than Beat 1 (> 3x area)
        val b1 = calibratedBeats.first()
        val b6 = calibratedBeats.last()
        assertTrue("Beat 6 area must be at least 3x Beat 1 area", b6.area >= 3 * b1.area)
        assertEquals("Beat 6 must reach full actuator saturation (255)", 255, b6.calibratedPeak)
    }

    @Test
    fun transient_preEmphasisAttackCompensation_providesLeadingKick() {
        val gearShift = HapticEvent(
            startTimeMs = 26540L,
            durationMs = 260L,
            intensity = 0.455f,
            semanticType = SemanticHapticType.GEAR_SHIFT,
            attackMs = 15L,
            sustainMs = 65L,
            releaseMs = 180L
        )
        val profile = SemanticHapticPatternGenerator.generateWaveformProfile(gearShift)
        val calibrated = calibration.calibrate(profile, gearShift)

        // The first step should be overdriven to overcome motor static inertia
        val firstStep = calibrated.amplitudes.first()
        assertTrue("Initial pre-emphasis step must be >= 145, was $firstStep", firstStep >= 145)
    }

    @Test
    fun continuousRumble_remainsSmoothWithoutAbruptPreEmphasisSpikes() {
        val rumble = HapticEvent(
            startTimeMs = 55200L,
            durationMs = 1200L,
            intensity = 0.612f,
            semanticType = SemanticHapticType.CONTINUOUS_RUMBLE,
            attackMs = 300L,
            sustainMs = 600L,
            releaseMs = 300L
        )
        val profile = SemanticHapticPatternGenerator.generateWaveformProfile(rumble)
        val calibrated = calibration.calibrate(profile, rumble)

        // Continuous rumble initial step should start gentle, not spike to max
        val firstStep = calibrated.amplitudes.first()
        assertTrue("Continuous rumble should start smoothly (< 80), was $firstStep", firstStep < 80)
        assertTrue("Continuous rumble should maintain consistent body", calibrated.rms >= 100.0)
    }

    @Test
    fun programmaticCalibrateAmplitude_matchesStep4Signature() {
        val result = calibration.calibrateAmplitude(
            sourceAmplitude = 0.760f,
            semanticType = SemanticHapticType.COLLISION,
            durationMs = 180L,
            attackMs = 15L,
            sustainMs = 65L,
            releaseMs = 100L
        )
        assertNotNull(result)
        assertEquals(255, result.calibratedPeak)
        assertEquals(0.760f, result.sourceIntensity, 0.001f)
        assertTrue(result.timings.isNotEmpty())
        assertTrue(result.amplitudes.isNotEmpty())
    }

    @Test
    fun uncalibratedPassthrough_preservesRawProfilesWhenDisabled() {
        val disabledCalib = DeviceHapticCalibration.UNCALIBRATED_PASSTHROUGH
        val event = HapticEvent(
            startTimeMs = 1000L,
            durationMs = 200L,
            intensity = 0.50f,
            semanticType = SemanticHapticType.SMOOTH
        )
        val profile = SemanticHapticPatternGenerator.generateWaveformProfile(event)
        val calibrated = disabledCalib.calibrate(profile, event)

        assertEquals(profile.amplitudes.toList(), calibrated.amplitudes.toList())
        assertEquals(profile.timings.toList(), calibrated.timings.toList())
    }
}
