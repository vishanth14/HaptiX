package com.haptix.app.data

import com.haptix.app.data.model.HapticFrequencyPoint
import com.haptix.app.data.parser.FrequencyPatternParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrequencyPatternParserTest {

    @Test
    fun parseValidJson_producesChronologicalPattern() {
        val json = """
            {
              "videoId": "vid_test_01",
              "source": "AOSP Guidelines",
              "sourceUrl": "https://source.android.com",
              "version": "1.0",
              "points": [
                { "startTimeMs": 2000, "durationMs": 100, "frequencyHz": 200.0, "amplitude": 0.8 },
                { "startTimeMs": 500, "durationMs": 50, "frequencyHz": 150.0, "amplitude": 0.5 },
                { "startTimeMs": 3500, "durationMs": 200, "frequencyHz": 235.0, "amplitude": 1.0 }
              ]
            }
        """.trimIndent()

        val result = FrequencyPatternParser.parseJson(json)
        assertTrue("Failed with: ${result.exceptionOrNull()?.message}, cause: ${result.exceptionOrNull()?.cause}", result.isSuccess)

        val pattern = result.getOrThrow()
        assertEquals("vid_test_01", pattern.videoId)
        assertEquals(3, pattern.pointCount)

        // Verify chronological sorting
        assertEquals(500L, pattern.points[0].startTimeMs)
        assertEquals(150.0f, pattern.points[0].frequencyHz)
        assertEquals(2000L, pattern.points[1].startTimeMs)
        assertEquals(200.0f, pattern.points[1].frequencyHz)
        assertEquals(3500L, pattern.points[2].startTimeMs)
        assertEquals(235.0f, pattern.points[2].frequencyHz)

        val hapticPattern = pattern.toHapticPattern()
        assertEquals(3, hapticPattern.eventCount)
        assertEquals(3700L, hapticPattern.totalDurationMs)
    }

    @Test
    fun parseInvalidFrequency_rejectsZeroNegativeOrNan() {
        val jsonZeroFreq = """
            {
              "points": [
                { "startTimeMs": 100, "durationMs": 50, "frequencyHz": 0.0, "amplitude": 0.5 }
              ]
            }
        """.trimIndent()
        assertFalse(FrequencyPatternParser.parseJson(jsonZeroFreq).isSuccess)

        val jsonNegFreq = """
            {
              "points": [
                { "startTimeMs": 100, "durationMs": 50, "frequencyHz": -150.0, "amplitude": 0.5 }
              ]
            }
        """.trimIndent()
        assertFalse(FrequencyPatternParser.parseJson(jsonNegFreq).isSuccess)
    }

    @Test
    fun parseInvalidDuration_rejectsNegative() {
        val jsonNegDur = """
            {
              "points": [
                { "startTimeMs": 100, "durationMs": -50, "frequencyHz": 200.0, "amplitude": 0.5 }
              ]
            }
        """.trimIndent()
        assertFalse(FrequencyPatternParser.parseJson(jsonNegDur).isSuccess)
    }

    @Test
    fun parseInvalidAmplitude_rejectsOutOfRange() {
        val jsonHighAmp = """
            {
              "points": [
                { "startTimeMs": 100, "durationMs": 50, "frequencyHz": 200.0, "amplitude": 1.5 }
              ]
            }
        """.trimIndent()
        assertFalse(FrequencyPatternParser.parseJson(jsonHighAmp).isSuccess)

        val jsonLowAmp = """
            {
              "points": [
                { "startTimeMs": 100, "durationMs": 50, "frequencyHz": 200.0, "amplitude": -0.1 }
              ]
            }
        """.trimIndent()
        assertFalse(FrequencyPatternParser.parseJson(jsonLowAmp).isSuccess)
    }

    @Test
    fun parseInvalidStartTime_rejectsNegative() {
        val jsonNegStart = """
            {
              "points": [
                { "startTimeMs": -100, "durationMs": 50, "frequencyHz": 200.0, "amplitude": 0.5 }
              ]
            }
        """.trimIndent()
        assertFalse(FrequencyPatternParser.parseJson(jsonNegStart).isSuccess)
    }

    @Test
    fun parseEmptyList_returnsEmptyPatternSafely() {
        val jsonEmpty = """
            {
              "videoId": "empty_stimulus",
              "points": []
            }
        """.trimIndent()

        val result = FrequencyPatternParser.parseJson(jsonEmpty)
        assertTrue("Failed with: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val pattern = result.getOrThrow()
        assertEquals(0, pattern.pointCount)
        assertEquals(0L, pattern.totalDurationMs)
    }

    @Test
    fun parseMalformedJson_failsGracefully() {
        val malformed = "{ this is not valid json: 123"
        val result = FrequencyPatternParser.parseJson(malformed)
        assertFalse(result.isSuccess)
        assertNotNull(result.exceptionOrNull())

        val empty = ""
        val emptyResult = FrequencyPatternParser.parseJson(empty)
        assertFalse(emptyResult.isSuccess)
    }

    @Test
    fun hapticFrequencyPoint_validationConstraints() {
        // Valid point
        val valid = HapticFrequencyPoint(
            startTimeMs = 0L,
            durationMs = 100L,
            frequencyHz = 200f,
            amplitude = 0.5f
        )
        assertEquals(100L, valid.endTimeMs)

        // Invalid frequency throws IllegalArgumentException
        try {
            HapticFrequencyPoint(startTimeMs = 0L, durationMs = 10L, frequencyHz = -10f, amplitude = 0.5f)
            org.junit.Assert.fail("Expected IllegalArgumentException for negative frequency")
        } catch (_: IllegalArgumentException) {}

        // Invalid amplitude throws IllegalArgumentException
        try {
            HapticFrequencyPoint(startTimeMs = 0L, durationMs = 10L, frequencyHz = 200f, amplitude = 1.2f)
            org.junit.Assert.fail("Expected IllegalArgumentException for amplitude > 1.0")
        } catch (_: IllegalArgumentException) {}
    }

    @Test
    fun parseProvenanceField_preservesProvenanceClassification() {
        val json = """
            {
              "videoId": "vid_provenance_test",
              "points": [
                {
                  "startTimeMs": 1000,
                  "durationMs": 100,
                  "frequencyHz": 200.0,
                  "amplitude": 0.9,
                  "description": "Pacinian benchmark",
                  "provenance": "perception-literature motivated"
                },
                {
                  "startTimeMs": 2500,
                  "durationMs": 80,
                  "frequencyHz": 235.0,
                  "amplitude": 0.85,
                  "description": "Experimental upper step",
                  "provenance": "experimentally selected by HaptiX"
                }
              ]
            }
        """.trimIndent()

        val result = FrequencyPatternParser.parseJson(json)
        assertTrue(result.isSuccess)

        val pattern = result.getOrThrow()
        assertEquals("perception-literature motivated", pattern.points[0].provenance)
        assertEquals("experimentally selected by HaptiX", pattern.points[1].provenance)

        val hapticEvent = pattern.points[0].toHapticEvent()
        assertEquals("perception-literature motivated", hapticEvent.parameters["provenance"])
    }

    @Test
    fun parseV2HapticRepresentation_parsesEnvelopeAndTextureObjects() {
        val v2Json = """
            {
              "videoId": "f1_2025_v2",
              "events": [
                {
                  "id": "f1_v2_002",
                  "startTimeMs": 7100,
                  "durationMs": 4100,
                  "semanticType": "ACCELERATION_RISE",
                  "priority": "HIGH",
                  "confidence": 0.94,
                  "intensity": 0.88,
                  "sharpness": 0.55,
                  "frequencyHz": 180.0,
                  "texture": {
                    "type": "ACCELERATION_RISE",
                    "modulationDepth": 0.18,
                    "modulationRateHz": 6.0,
                    "isContinuous": true
                  },
                  "envelope": {
                    "attackMs": 600,
                    "sustainMs": 2700,
                    "releaseMs": 800,
                    "curveIn": "EASE_IN",
                    "curveOut": "EASE_OUT",
                    "totalMs": 4100
                  }
                }
              ]
            }
        """.trimIndent()

        val result = FrequencyPatternParser.parseToHapticPattern(v2Json)
        assertTrue("V2 JSON should parse successfully: ${result.exceptionOrNull()?.message}", result.isSuccess)

        val pattern = result.getOrThrow()
        assertEquals(1, pattern.eventCount)
        val event = pattern.events[0]
        assertEquals(7100L, event.startTimeMs)
        assertEquals(4100L, event.durationMs)
        assertEquals(0.88f, event.intensity, 0.01f)

        val env = event.envelope
        assertNotNull(env)
        assertEquals(600L, env?.attackMs)
        assertEquals(2700L, env?.sustainMs)
        assertEquals(800L, env?.releaseMs)
        assertEquals("EASE_IN", env?.curveIn)
        assertEquals("EASE_OUT", env?.curveOut)

        val tex = event.parameters["texture"] as? Map<*, *>
        assertNotNull(tex)
        assertEquals("ACCELERATION_RISE", tex?.get("type"))
    }

    @Test
    fun parseV3HapticRepresentation_parsesClassificationAndTransientDurations() {
        val v3Json = """
            {
              "videoId": "f1_2025",
              "version": "3.0.0",
              "events": [
                {
                  "id": "f1_v3_012",
                  "startTimeMs": 67200,
                  "durationMs": 160,
                  "semanticType": "GEAR_SHIFT",
                  "classification": "TRANSIENT",
                  "layerRole": "ACCENT",
                  "priority": "HIGH",
                  "confidence": 0.94,
                  "intensity": 0.80,
                  "sharpness": 0.84,
                  "frequencyHz": 210.0,
                  "attackMs": 25,
                  "sustainMs": 55,
                  "releaseMs": 80,
                  "curveIn": "LINEAR",
                  "curveOut": "EASE_OUT",
                  "envelope": {
                    "attackMs": 25,
                    "sustainMs": 55,
                    "releaseMs": 80,
                    "curveIn": "LINEAR",
                    "curveOut": "EASE_OUT",
                    "totalMs": 160
                  },
                  "texture": {
                    "type": "GEAR_SHIFT",
                    "classification": "TRANSIENT",
                    "isContinuous": false
                  }
                },
                {
                  "id": "f1_v3_020",
                  "startTimeMs": 99360,
                  "durationMs": 1800,
                  "semanticType": "CRASH_AFTERSHOCK",
                  "classification": "AFTERMATH",
                  "layerRole": "AFTERMATH",
                  "priority": "MEDIUM",
                  "confidence": 0.90,
                  "intensity": 0.58,
                  "sharpness": 0.34,
                  "frequencyHz": 85.0,
                  "envelope": {
                    "attackMs": 216,
                    "sustainMs": 504,
                    "releaseMs": 1080,
                    "curveIn": "EASE_IN",
                    "curveOut": "SMOOTHSTEP",
                    "totalMs": 1800
                  }
                }
              ]
            }
        """.trimIndent()

        val result = FrequencyPatternParser.parseToHapticPattern(v3Json)
        assertTrue(result.isSuccess)

        val pattern = result.getOrThrow()
        assertEquals(2, pattern.eventCount)

        // Transient gear shift
        val shift = pattern.events[0]
        assertEquals("f1_v3_012", shift.id)
        assertEquals(160L, shift.durationMs)
        assertEquals("TRANSIENT", shift.parameters["classification"])
        assertEquals("ACCENT", shift.parameters["layerRole"])
        assertEquals(25L, shift.envelope.attackMs)
        assertEquals(55L, shift.envelope.sustainMs)
        assertEquals(80L, shift.envelope.releaseMs)

        // Aftermath rumble
        val crashAftermath = pattern.events[1]
        assertEquals("f1_v3_020", crashAftermath.id)
        assertEquals(1800L, crashAftermath.durationMs)
        assertEquals("AFTERMATH", crashAftermath.parameters["classification"])
        assertEquals(0.58f, crashAftermath.intensity, 0.01f)
        assertEquals("SMOOTHSTEP", crashAftermath.envelope.curveOut)
    }
}

