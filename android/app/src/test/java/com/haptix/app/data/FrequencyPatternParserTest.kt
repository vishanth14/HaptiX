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
}

