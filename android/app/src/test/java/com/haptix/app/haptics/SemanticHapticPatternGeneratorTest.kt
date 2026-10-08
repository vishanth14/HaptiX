package com.haptix.app.haptics

import com.haptix.app.data.model.HapticEnvelope
import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.SemanticHapticType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Validates the [SemanticHapticPatternGenerator] envelope synthesis,
 * constraint verification, and smooth waveform generation.
 */
class SemanticHapticPatternGeneratorTest {

    @Test
    fun allThirteenSemanticTypes_resolveAndGenerateValidEnvelopes() {
        val allTypes = SemanticHapticType.entries

        for (type in allTypes) {
            val event = HapticEvent(
                startTimeMs = 1000L,
                durationMs = 800L,
                intensity = 0.85f,
                parameters = mapOf(
                    "hapticType" to type.name,
                    "frequencyHz" to 190.0f
                )
            )

            val resolvedType = SemanticHapticPatternGenerator.resolveSemanticType(event)
            assertEquals(type, resolvedType)

            val envelope = SemanticHapticPatternGenerator.resolveEnvelope(event, resolvedType)
            assertTrue("attackMs for $type must be >= 0", envelope.attackMs >= 0L)
            assertTrue("sustainMs for $type must be >= 0", envelope.sustainMs >= 0L)
            assertTrue("releaseMs for $type must be >= 0", envelope.releaseMs >= 0L)
            assertTrue(
                "Envelope sum (${envelope.totalMs}ms) for $type must be <= duration (800ms)",
                envelope.totalMs <= 800L
            )

            val profile = SemanticHapticPatternGenerator.generateWaveformProfile(event)
            assertEquals(type, profile.semanticType)
            assertTrue("Timings array must not be empty", profile.timings.isNotEmpty())
            assertTrue("Amplitudes array must not be empty", profile.amplitudes.isNotEmpty())
            assertEquals(
                "Timings and amplitudes must have matching lengths",
                profile.timings.size,
                profile.amplitudes.size
            )
        }
    }

    @Test
    fun smoothEnvelope_exhibitsFadeInAndFadeOutWithoutAbruptSpikes() {
        val event = HapticEvent(
            startTimeMs = 2000L,
            durationMs = 900L,
            intensity = 0.80f,
            parameters = mapOf(
                "hapticType" to "SMOOTH",
                "frequencyHz" to 175.0f,
                "pattern" to mapOf("attackMs" to 300L, "sustainMs" to 300L, "releaseMs" to 300L)
            )
        )

        val profile = SemanticHapticPatternGenerator.generateWaveformProfile(event)
        val amps = profile.amplitudes

        // 1. Initial amplitude must be a controlled fade-in (not instant maximum)
        assertTrue("Initial amplitude must be soft fade-in (< 40% of max 255)", amps.first() < 100)

        // 2. Middle amplitudes must reach peak
        val maxAmp = amps.maxOrNull() ?: 0
        assertTrue("Max amplitude must reach peak body", maxAmp >= 180)

        // 3. Final amplitude must be faded out to 0 or near-zero
        assertTrue("Final amplitude must fade out (<= 25)", amps.last() <= 25)

        // 4. No abrupt step in attack: verify consecutive differences during attack are smooth
        val attackSteps = (300L / 25L).toInt()
        for (i in 0 until attackSteps - 1) {
            val diff = amps[i + 1] - amps[i]
            assertTrue("Attack amplitude must ramp monotonically or smoothly, diff: $diff", diff >= 0)
        }
    }

    @Test
    fun heavyImpact_usesShortAttackBodyAndDecayInsteadOfInstantSpike() {
        val event = HapticEvent(
            startTimeMs = 5000L,
            durationMs = 800L,
            intensity = 0.90f,
            parameters = mapOf(
                "hapticType" to "HEAVY_IMPACT",
                "attackMs" to 120L,
                "sustainMs" to 180L,
                "releaseMs" to 500L
            )
        )

        val profile = SemanticHapticPatternGenerator.generateWaveformProfile(event)
        val amps = profile.amplitudes

        // Does NOT start at max
        assertTrue("Heavy impact must have controlled attack, not instant 255", amps.first() < 200)

        // Sustains strong peak
        val peak = amps.maxOrNull() ?: 0
        assertTrue("Heavy impact must deliver strong peak (>= 220)", peak >= 220)

        // Ends with natural decay
        assertTrue("Heavy impact must decay to 0", amps.last() == 0)
    }

    @Test
    fun softImpactAndEmotionalSwell_clampIntensityToGentleLevels() {
        // Soft impact given 1.0 intensity should be clamped
        val softEvent = HapticEvent(
            startTimeMs = 1000L,
            durationMs = 400L,
            intensity = 1.0f,
            parameters = mapOf("hapticType" to "SOFT_IMPACT")
        )
        val softProfile = SemanticHapticPatternGenerator.generateWaveformProfile(softEvent)
        assertTrue("Soft impact must be clamped below 0.60, was ${softProfile.peakAmplitude}", softProfile.peakAmplitude <= 0.55f)

        // Emotional swell given 1.0 intensity should be clamped
        val swellEvent = HapticEvent(
            startTimeMs = 2000L,
            durationMs = 1200L,
            intensity = 1.0f,
            parameters = mapOf("hapticType" to "EMOTIONAL_SWELL")
        )
        val swellProfile = SemanticHapticPatternGenerator.generateWaveformProfile(swellEvent)
        assertTrue("Emotional swell must be clamped below 0.55, was ${swellProfile.peakAmplitude}", swellProfile.peakAmplitude <= 0.50f)
    }

    @Test
    fun doublePulse_containsTwoEnvelopesAndQuietGap() {
        val event = HapticEvent(
            startTimeMs = 10000L,
            durationMs = 600L,
            intensity = 0.85f,
            parameters = mapOf("hapticType" to "DOUBLE_PULSE")
        )

        val profile = SemanticHapticPatternGenerator.generateWaveformProfile(event)
        val amps = profile.amplitudes

        // Should contain at least one internal 0 representing the gap between pulses
        val zeroIndices = amps.indices.filter { amps[it] == 0 }
        assertTrue("Double pulse must have an internal silent gap", zeroIndices.isNotEmpty())

        // Peak before gap and peak after gap
        val mid = zeroIndices.first()
        val peak1 = amps.slice(0 until mid).maxOrNull() ?: 0
        val peak2 = amps.slice(mid until amps.size).maxOrNull() ?: 0

        assertTrue("First pulse must deliver tactile sensation", peak1 > 100)
        assertTrue("Second pulse must deliver tactile sensation", peak2 > 50)
    }

    @Test
    fun invalidEnvelopeParameters_throwIllegalArgumentException() {
        // Total exceeds duration
        val invalidEnvelope = HapticEnvelope(attackMs = 300L, sustainMs = 300L, releaseMs = 300L)
        try {
            invalidEnvelope.validateForDuration(800L)
            fail("Expected IllegalArgumentException when envelope exceeds duration")
        } catch (_: IllegalArgumentException) {}

        // Negative parameter
        try {
            HapticEnvelope(attackMs = -10L, sustainMs = 100L, releaseMs = 100L)
            fail("Expected IllegalArgumentException for negative attackMs")
        } catch (_: IllegalArgumentException) {}
    }

    @Test
    fun jump_exhibitsFourPhaseKinematicMotionWithoutAbruptSpike() {
        val event = HapticEvent(
            startTimeMs = 50200L,
            durationMs = 1300L,
            intensity = 0.85f,
            parameters = mapOf(
                "hapticType" to "JUMP",
                "frequencyHz" to 170.0f
            )
        )

        val profile = SemanticHapticPatternGenerator.generateWaveformProfile(event)
        val amps = profile.amplitudes
        val timings = profile.timings

        assertEquals(SemanticHapticType.JUMP, profile.semanticType)
        assertTrue("JUMP envelope attackMs must be > 0", profile.envelope.attackMs > 0)
        assertTrue("JUMP envelope releaseMs must be > 0", profile.envelope.releaseMs > 0)

        // 1. Anticipation start must be gentle (not instant maximum)
        assertTrue("Jump anticipation must begin softly (< 30% of 255)", amps.first() < 80)

        // 2. Launch surge reaches high peak
        val maxAmp = amps.maxOrNull() ?: 0
        assertTrue("Launch surge must reach significant amplitude (>= 180)", maxAmp >= 180)

        // 3. Landing decay fades to 0
        assertEquals("Final landing decay must reach 0", 0, amps.last())

        // 4. Total duration matches
        val totalMs = timings.sum()
        assertEquals(1300L, totalMs)
    }

    @Test
    fun launch_exhibitsGradualBuildAndAirborneDecay() {
        val event = HapticEvent(
            startTimeMs = 57600L,
            durationMs = 1100L,
            intensity = 0.85f,
            parameters = mapOf(
                "hapticType" to "LAUNCH",
                "frequencyHz" to 195.0f
            )
        )

        val profile = SemanticHapticPatternGenerator.generateWaveformProfile(event)
        val amps = profile.amplitudes

        assertEquals(SemanticHapticType.LAUNCH, profile.semanticType)
        assertTrue("LAUNCH envelope attackMs must be > 0", profile.envelope.attackMs > 0)
        assertTrue("LAUNCH envelope releaseMs must be > 0", profile.envelope.releaseMs > 0)

        // Does not start at max
        assertTrue("Launch must start with gradual build (< 40% of 255)", amps.first() < 100)

        // Strong peak body
        val peak = amps.maxOrNull() ?: 0
        assertTrue("Launch must achieve strong peak (>= 180)", peak >= 180)

        // Decays to 0
        assertEquals("Launch must decay to 0", 0, amps.last())
    }

    @Test
    fun envelopeCurves_applyCorrectMathematicalRamps() {
        val curves = listOf(
            "LINEAR", "SMOOTHSTEP", "EASE_IN", "EASE_OUT",
            "EASE_IN_OUT", "EXPONENTIAL", "LOGARITHMIC"
        )
        for (curve in curves) {
            val atZero = SemanticHapticPatternGenerator.applyCurve(curve, 0.0f)
            val atOne = SemanticHapticPatternGenerator.applyCurve(curve, 1.0f)
            val atMid = SemanticHapticPatternGenerator.applyCurve(curve, 0.5f)

            assertEquals("Curve $curve at 0.0 must be 0.0", 0.0f, atZero, 0.01f)
            assertEquals("Curve $curve at 1.0 must be 1.0", 1.0f, atOne, 0.01f)
            assertTrue("Curve $curve at 0.5 must be between 0 and 1: $atMid", atMid in 0.0f..1.0f)
        }
    }

    @Test
    fun v2AccelerationRise_producesSmoothstepWaveformWithModulation() {
        val event = HapticEvent(
            startTimeMs = 7100L,
            durationMs = 4100L,
            intensity = 0.88f,
            parameters = mapOf(
                "hapticType" to "ACCELERATION_RISE",
                "frequencyHz" to 180.0f,
                "envelope" to mapOf(
                    "attackMs" to 600L,
                    "sustainMs" to 2700L,
                    "releaseMs" to 800L,
                    "curveIn" to "EASE_IN",
                    "curveOut" to "EASE_OUT"
                ),
                "texture" to mapOf(
                    "type" to "ACCELERATION_RISE",
                    "modulationDepth" to 0.18f,
                    "modulationRateHz" to 6.0f,
                    "isContinuous" to true
                )
            )
        )

        val profile = SemanticHapticPatternGenerator.generateWaveformProfile(event)
        assertEquals(SemanticHapticType.ACCELERATION_RISE, profile.semanticType)
        assertTrue("Timings must be populated", profile.timings.isNotEmpty())
        assertEquals(profile.timings.size, profile.amplitudes.size)

        // Envelope attack, sustain, release must match V2 parameters
        assertEquals(600L, profile.envelope.attackMs)
        assertEquals(2700L, profile.envelope.sustainMs)
        assertEquals(800L, profile.envelope.releaseMs)

        // First amplitude must be gentle fade in
        assertTrue("Fade-in must start gently (< 120)", profile.amplitudes.first() < 120)

        // Middle amplitude reaches strong peak
        val maxAmp = profile.amplitudes.maxOrNull() ?: 0
        assertTrue("Sustain must reach strong peak (>= 180)", maxAmp >= 180)

        // Final amplitude fades out to 0
        assertEquals("Release must decay to 0", 0, profile.amplitudes.last())
    }

    @Test
    fun malformedData_fallsBackGracefullyWithoutCrash() {
        val malformedEvent = HapticEvent(
            startTimeMs = 0L,
            durationMs = 0L,
            intensity = 0.0f,
            parameters = mapOf(
                "hapticType" to "UNKNOWN_NON_EXISTENT_TYPE",
                "frequencyHz" to -999.0f,
                "envelope" to "NotAMap"
            )
        )
        // Should not crash and should produce non-empty safe waveform
        val profile = SemanticHapticPatternGenerator.generateWaveformProfile(malformedEvent)
        assertNotNull(profile)
        assertTrue(profile.timings.isNotEmpty())
        assertTrue(profile.amplitudes.isNotEmpty())
    }
}
