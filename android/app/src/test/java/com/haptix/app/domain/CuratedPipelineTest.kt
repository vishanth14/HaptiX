package com.haptix.app.domain

import com.haptix.app.data.model.HapticEventType
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.model.SemanticHapticType
import com.haptix.app.data.parser.FrequencyPatternParser
import com.haptix.app.data.repository.DefaultHapticRepository
import com.haptix.app.data.repository.DefaultVideoRepository
import com.haptix.app.domain.model.CuratedVideo
import com.haptix.app.domain.model.HapticSource
import com.haptix.app.domain.model.VideoSource
import com.haptix.app.domain.provider.CuratedHapticProvider
import com.haptix.app.haptics.HapticEngine
import com.haptix.app.haptics.HapticSynchronizer
import com.haptix.app.platform.android.AndroidHapticConverter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Generic Curated Reference Architecture Test Suite.
 *
 * Verifies that the curated video infrastructure, providers, HapticPattern loading,
 * synchronization, and platform converter remain intact and fully operational
 * for real movie scenes without synthetic placeholders.
 */
class CuratedPipelineTest {

    private val sampleCuratedJson = """
        {
          "schemaVersion": "2.0.0",
          "videoId": "curated_demo_01",
          "title": "Reference Video Scene 1",
          "source": "CURATED",
          "events": [
            {
              "id": "e1-0001",
              "startTimeMs": 1200,
              "durationMs": 400,
              "semanticType": "LAUNCH",
              "intensity": 0.85,
              "sharpness": 0.65,
              "attackMs": 100,
              "sustainMs": 200,
              "releaseMs": 100,
              "frequencyHz": 210.0,
              "confidence": 0.95
            },
            {
              "id": "e1-0002",
              "startTimeMs": 3500,
              "durationMs": 250,
              "semanticType": "HEAVY_IMPACT",
              "intensity": 0.90,
              "sharpness": 0.70,
              "attackMs": 50,
              "sustainMs": 100,
              "releaseMs": 100,
              "frequencyHz": 200.0,
              "confidence": 0.98
            }
          ]
        }
    """.trimIndent()

    @Test
    fun testCuratedProviderLoadsValidPattern() = runBlocking {
        val provider = CuratedHapticProvider(
            assetReader = { path -> if (path.contains("curated_demo_01")) sampleCuratedJson else null }
        )
        val pattern = provider.getPattern("curated_demo_01")
        assertNotNull("Curated pattern must be loaded", pattern)
        assertEquals(HapticSource.CURATED, pattern!!.source)
        assertEquals("curated_demo_01", pattern.videoId)
        assertEquals(2, pattern.eventCount)
    }

    @Test
    fun testCuratedEnvelopeValidityAndOrdering() = runBlocking {
        val parseResult = FrequencyPatternParser.parseToHapticPattern(sampleCuratedJson)
        assertTrue("Parsing must succeed", parseResult.isSuccess)
        val pattern = parseResult.getOrThrow()

        for (event in pattern.events) {
            val env = event.envelope
            assertTrue("attackMs must be >= 0", env.attackMs >= 0L)
            assertTrue("sustainMs must be >= 0", env.sustainMs >= 0L)
            assertTrue("releaseMs must be >= 0", env.releaseMs >= 0L)
            assertTrue(
                "Envelope sum (${env.totalMs}ms) must be <= duration (${event.durationMs}ms)",
                env.totalMs <= event.durationMs
            )
        }

        // Chronological order
        for (i in 0 until pattern.events.size - 1) {
            assertTrue(
                "Events must be chronologically sorted",
                pattern.events[i].startTimeMs <= pattern.events[i + 1].startTimeMs
            )
        }
    }

    @Test
    fun testCuratedAndroidConversionCompatibility() = runBlocking {
        val parseResult = FrequencyPatternParser.parseToHapticPattern(sampleCuratedJson)
        val pattern = parseResult.getOrThrow()
        val freqPattern = AndroidHapticConverter.toAndroidFrequencyPattern(pattern)

        assertEquals(pattern.videoId, freqPattern.videoId)
        assertEquals(pattern.events.size, freqPattern.points.size)

        for (i in pattern.events.indices) {
            val event = pattern.events[i]
            val point = freqPattern.points[i]
            assertEquals(event.startTimeMs, point.startTimeMs)
            assertEquals(event.durationMs, point.durationMs)
            assertEquals(event.intensity, point.amplitude, 0.001f)
            val freq = (point.frequencyHz)
            assertTrue(freq in 50f..500f)
        }
    }

    @Test
    fun testSynchronizationBehaviorWithCuratedPattern() = runBlocking {
        val parseResult = FrequencyPatternParser.parseToHapticPattern(sampleCuratedJson)
        val pattern = parseResult.getOrThrow()

        val triggeredEvents = mutableListOf<com.haptix.app.data.model.HapticEvent>()
        val mockEngine = object : HapticEngine {
            override fun isHapticSupported(): Boolean = true
            override fun loadPattern(pattern: HapticPattern) {}
            override fun start() {}
            override fun stop() {}
            override fun release() {}
            override fun playEvent(event: com.haptix.app.data.model.HapticEvent) {
                triggeredEvents.add(event)
            }
        }

        val synchronizer = HapticSynchronizer(mockEngine, toleranceMs = 50L)
        synchronizer.setPattern(pattern)

        // Advance before first event
        synchronizer.onTimelineUpdate(500L)
        assertEquals(0, triggeredEvents.size)

        // Advance through first event (1200ms)
        synchronizer.onTimelineUpdate(1210L)
        assertEquals(1, triggeredEvents.size)
        assertEquals("e1-0001", triggeredEvents[0].id)

        // Advance to second event (3500ms)
        synchronizer.onTimelineUpdate(3505L)
        assertEquals(2, triggeredEvents.size)
        assertEquals("e1-0002", triggeredEvents[1].id)
    }

    @Test
    fun testCuratedRepositoryDefaults() = runBlocking {
        val repo = DefaultVideoRepository()
        val curated = repo.getCuratedVideos().first()
        assertTrue("Curated list is empty until real movie scenes are added", curated.isEmpty())

        val videos = repo.getVideos().first()
        assertTrue("Repository provides Koji and research stimuli", videos.isNotEmpty())
        assertTrue("Koji video is present", videos.any { it.id == "koji" })
    }

    @Test
    fun testMissingResourceHandling() = runBlocking {
        val provider = CuratedHapticProvider(assetReader = { null })
        val result = provider.getPattern("non_existent_curated_id")
        assertNull("Missing resource must cleanly return null", result)
    }
}
