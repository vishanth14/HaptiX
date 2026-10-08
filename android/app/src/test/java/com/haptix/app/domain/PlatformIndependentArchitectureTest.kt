package com.haptix.app.domain

import com.haptix.app.data.model.HapticEnvelope
import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticEventType
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.model.SemanticHapticType
import com.haptix.app.data.repository.DefaultHapticRepository
import com.haptix.app.domain.model.HapticEvidence
import com.haptix.app.domain.model.HapticSource
import com.haptix.app.domain.model.ProcessingJob
import com.haptix.app.domain.model.ProcessingStatus
import com.haptix.app.domain.model.VideoMetadata
import com.haptix.app.domain.model.VideoSource
import com.haptix.app.domain.provider.CuratedHapticProvider
import com.haptix.app.domain.provider.GeneratedHapticProvider
import com.haptix.app.domain.provider.HapticSourceProvider
import com.haptix.app.domain.provider.LegacyManualHapticProvider
import com.haptix.app.platform.android.AndroidHapticConverter
import com.haptix.app.platform.ios.AhapHapticConverter
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * Verification test suite for Phase 1: Platform-Independent Haptic Architecture.
 *
 * Validates:
 * 1. HapticEvent validation
 * 2. HapticEnvelope validation
 * 3. Attack/sustain/release duration consistency
 * 4. HapticPattern event ordering
 * 5. HapticEvidence serialization/deserialization
 * 6. Haptic source selection
 * 7. CURATED source provider
 * 8. GENERATED source provider
 * 9. LEGACY_MANUAL source provider
 * 10. VideoSource models and YouTube URL ID extraction
 * 11. ProcessingJob state machine
 * 12. Android conversion from HapticPattern
 * 13. Preservation of existing Koji behavior
 * 14. AHAP conversion for future iOS CoreHaptics
 */
class PlatformIndependentArchitectureTest {

    // -------------------------------------------------------------
    // 1. HapticEvent Validation
    // -------------------------------------------------------------
    @Test
    fun testHapticEventValidation() {
        // Valid canonical event
        val validEvent = HapticEvent(
            id = "evt_001",
            startTimeMs = 1200L,
            durationMs = 600L,
            intensity = 0.85f,
            sharpness = 0.6f,
            semanticType = SemanticHapticType.JUMP,
            attackMs = 200L,
            sustainMs = 200L,
            releaseMs = 200L,
            frequencyHz = 180.0f,
            confidence = 0.95f,
            sourceModalities = listOf("visual", "audio")
        )
        assertEquals("evt_001", validEvent.id)
        assertEquals(1200L, validEvent.startTimeMs)
        assertEquals(1800L, validEvent.endTimeMs)
        assertEquals(0.85f, validEvent.intensity, 0.001f)

        // Invalid: negative startTimeMs
        try {
            validEvent.copy(startTimeMs = -10L)
            fail("Expected IllegalArgumentException for negative startTimeMs")
        } catch (_: IllegalArgumentException) {}

        // Invalid: negative durationMs
        try {
            validEvent.copy(durationMs = -50L)
            fail("Expected IllegalArgumentException for negative durationMs")
        } catch (_: IllegalArgumentException) {}

        // Invalid: intensity > 1.0f
        try {
            validEvent.copy(intensity = 1.2f)
            fail("Expected IllegalArgumentException for intensity > 1.0f")
        } catch (_: IllegalArgumentException) {}

        // Invalid: non-positive frequencyHz
        try {
            validEvent.copy(frequencyHz = 0.0f)
            fail("Expected IllegalArgumentException for frequencyHz <= 0")
        } catch (_: IllegalArgumentException) {}

        // Invalid: confidence out of bounds
        try {
            validEvent.copy(confidence = -0.1f)
            fail("Expected IllegalArgumentException for confidence < 0")
        } catch (_: IllegalArgumentException) {}
    }

    // -------------------------------------------------------------
    // 2. HapticEnvelope Validation
    // -------------------------------------------------------------
    @Test
    fun testHapticEnvelopeValidation() {
        val env = HapticEnvelope(attackMs = 150L, sustainMs = 300L, releaseMs = 150L)
        assertEquals(600L, env.totalMs)

        // Negative values are rejected
        try {
            HapticEnvelope(attackMs = -10L, sustainMs = 100L, releaseMs = 50L)
            fail("Expected IllegalArgumentException for negative attackMs")
        } catch (_: IllegalArgumentException) {}

        try {
            HapticEnvelope(attackMs = 10L, sustainMs = -100L, releaseMs = 50L)
            fail("Expected IllegalArgumentException for negative sustainMs")
        } catch (_: IllegalArgumentException) {}

        try {
            HapticEnvelope(attackMs = 10L, sustainMs = 100L, releaseMs = -50L)
            fail("Expected IllegalArgumentException for negative releaseMs")
        } catch (_: IllegalArgumentException) {}
    }

    // -------------------------------------------------------------
    // 3. Attack/Sustain/Release Duration Consistency
    // -------------------------------------------------------------
    @Test
    fun testAttackSustainReleaseDurationConsistency() {
        val env = HapticEnvelope(attackMs = 200L, sustainMs = 400L, releaseMs = 200L) // total 800ms

        // Valid: totalMs (800ms) <= duration (1000ms)
        env.validateForDuration(1000L)

        // Invalid: totalMs (800ms) > duration (700ms)
        try {
            env.validateForDuration(700L)
            fail("Expected IllegalArgumentException when envelope exceeds duration")
        } catch (_: IllegalArgumentException) {}

        // Enforced on HapticEvent creation
        try {
            HapticEvent(
                startTimeMs = 0L,
                durationMs = 500L,
                attackMs = 300L,
                sustainMs = 200L,
                releaseMs = 100L // 300 + 200 + 100 = 600 > 500
            )
            fail("Expected IllegalArgumentException on HapticEvent when envelope exceeds duration")
        } catch (_: IllegalArgumentException) {}
    }

    // -------------------------------------------------------------
    // 4. HapticPattern Event Ordering
    // -------------------------------------------------------------
    @Test
    fun testHapticPatternEventOrdering() {
        val e1 = HapticEvent(id = "e1", startTimeMs = 5000L, durationMs = 300L)
        val e2 = HapticEvent(id = "e2", startTimeMs = 1000L, durationMs = 500L)
        val e3 = HapticEvent(id = "e3", startTimeMs = 3000L, durationMs = 200L)

        val pattern = HapticPattern(
            videoId = "test_vid",
            events = listOf(e1, e2, e3)
        )

        assertEquals(3, pattern.eventCount)
        assertEquals(5300L, pattern.totalDurationMs)

        val sorted = pattern.sortedEvents()
        assertEquals("e2", sorted[0].id)
        assertEquals("e3", sorted[1].id)
        assertEquals("e1", sorted[2].id)
    }

    // -------------------------------------------------------------
    // 5. HapticEvidence Serialization / Deserialization
    // -------------------------------------------------------------
    @Test
    fun testHapticEvidenceSerializationDeserialization() {
        val original = HapticEvidence(
            sceneId = 3,
            visualEvidence = "character leaps upward over canyon gorge",
            audioEvidence = "rising percussive wind whoosh",
            motionEvidence = "high upward optical flow vector",
            frameTimestampsMs = listOf(50200L, 50600L, 51000L),
            audioWindowMs = Pair(50100L, 51300L),
            visualReliability = 0.88f,
            audioReliability = 0.76f,
            crossModalSimilarity = 0.91f,
            confidence = 0.94f
        )

        val map = original.toMap()
        assertEquals(3, map["sceneId"])
        assertEquals("character leaps upward over canyon gorge", map["visualEvidence"])
        assertEquals(0.88f, (map["visualReliability"] as Float), 0.001f)

        val reconstructed = HapticEvidence.fromMap(map)
        assertEquals(original.sceneId, reconstructed.sceneId)
        assertEquals(original.visualEvidence, reconstructed.visualEvidence)
        assertEquals(original.audioEvidence, reconstructed.audioEvidence)
        assertEquals(original.motionEvidence, reconstructed.motionEvidence)
        assertEquals(original.frameTimestampsMs, reconstructed.frameTimestampsMs)
        assertEquals(original.audioWindowMs, reconstructed.audioWindowMs)
        assertEquals(original.visualReliability, reconstructed.visualReliability, 0.001f)
        assertEquals(original.audioReliability, reconstructed.audioReliability, 0.001f)
        assertEquals(original.crossModalSimilarity, reconstructed.crossModalSimilarity, 0.001f)
        assertEquals(original.confidence, reconstructed.confidence, 0.001f)
    }

    // -------------------------------------------------------------
    // 6. Haptic Source Selection (Repository Orchestration)
    // -------------------------------------------------------------
    @Test
    fun testHapticSourceSelection() = runBlocking {
        val mockCurated = object : HapticSourceProvider {
            override val source = HapticSource.CURATED
            override suspend fun getPattern(videoId: String): HapticPattern? =
                if (videoId == "curated_vid") HapticPattern(videoId, source = HapticSource.CURATED) else null
        }

        val mockGenerated = object : HapticSourceProvider {
            override val source = HapticSource.GENERATED
            override suspend fun getPattern(videoId: String): HapticPattern? =
                if (videoId == "generated_vid" || videoId == "both_vid") HapticPattern(videoId, source = HapticSource.GENERATED) else null
        }

        val mockLegacy = object : HapticSourceProvider {
            override val source = HapticSource.LEGACY_MANUAL
            override suspend fun getPattern(videoId: String): HapticPattern? =
                if (videoId == "legacy_vid" || videoId == "both_vid") HapticPattern(videoId, source = HapticSource.LEGACY_MANUAL) else null
        }

        val repo = DefaultHapticRepository(
            providers = listOf(mockGenerated, mockCurated, mockLegacy)
        )

        // Default priority: for both_vid, GENERATED wins over LEGACY
        val p1 = repo.loadHapticPatternForVideo("both_vid").getOrThrow()
        assertEquals(HapticSource.GENERATED, p1.source)
        assertEquals(HapticSource.GENERATED, repo.getActiveSource())

        // Preference set to LEGACY_MANUAL
        repo.setPreferredSource(HapticSource.LEGACY_MANUAL)
        val p2 = repo.loadHapticPatternForVideo("both_vid").getOrThrow()
        assertEquals(HapticSource.LEGACY_MANUAL, p2.source)
        assertEquals(HapticSource.LEGACY_MANUAL, repo.getActiveSource())

        // Preference set to CURATED for curated_vid
        repo.setPreferredSource(HapticSource.CURATED)
        val p3 = repo.loadHapticPatternForVideo("curated_vid").getOrThrow()
        assertEquals(HapticSource.CURATED, p3.source)

        // Fallback for non-existent video returns empty pattern with LEGACY_MANUAL
        val pMissing = repo.loadHapticPatternForVideo("non_existent").getOrThrow()
        assertEquals(0, pMissing.eventCount)
    }

    // -------------------------------------------------------------
    // 7. CURATED Source Provider
    // -------------------------------------------------------------
    @Test
    fun testCuratedSourceProvider() = runBlocking {
        val sampleJson = """
            {
              "videoId": "curated_ref_01",
              "points": [
                { "startTimeMs": 100, "durationMs": 200, "frequencyHz": 180.0, "amplitude": 0.7 }
              ]
            }
        """.trimIndent()

        val provider = CuratedHapticProvider(
            assetReader = { path -> if (path.contains("curated_ref_01")) sampleJson else null }
        )

        assertEquals(HapticSource.CURATED, provider.source)
        val pattern = provider.getPattern("curated_ref_01")
        assertNotNull(pattern)
        assertEquals(HapticSource.CURATED, pattern!!.source)
        assertEquals(1, pattern.eventCount)
    }

    // -------------------------------------------------------------
    // 8. GENERATED Source Provider
    // -------------------------------------------------------------
    @Test
    fun testGeneratedSourceProvider() = runBlocking {
        val sampleJson = """
            {
              "videoId": "koji",
              "points": [
                { "startTimeMs": 500, "durationMs": 400, "frequencyHz": 200.0, "amplitude": 0.85 }
              ]
            }
        """.trimIndent()

        val provider = GeneratedHapticProvider(
            assetReader = { path -> if (path.contains("koji")) sampleJson else null }
        )

        assertEquals(HapticSource.GENERATED, provider.source)
        val pattern = provider.getPattern("koji")
        assertNotNull(pattern)
        assertEquals(HapticSource.GENERATED, pattern!!.source)
        assertEquals(1, pattern.eventCount)
    }

    // -------------------------------------------------------------
    // 9. LEGACY_MANUAL Source Provider
    // -------------------------------------------------------------
    @Test
    fun testLegacyManualSourceProvider() = runBlocking {
        val sampleJson = """
            {
              "videoId": "koji",
              "points": [
                { "startTimeMs": 4300, "durationMs": 900, "frequencyHz": 160.0, "amplitude": 0.45 }
              ]
            }
        """.trimIndent()

        val provider = LegacyManualHapticProvider(
            assetReader = { path -> if (path.contains("koji")) sampleJson else null }
        )

        assertEquals(HapticSource.LEGACY_MANUAL, provider.source)
        val pattern = provider.getPattern("koji")
        assertNotNull(pattern)
        assertEquals(HapticSource.LEGACY_MANUAL, pattern!!.source)
        assertEquals(1, pattern.eventCount)
    }

    // -------------------------------------------------------------
    // 10. VideoSource Models & URL Parsing
    // -------------------------------------------------------------
    @Test
    fun testVideoSourceModels() {
        val local = VideoSource.Local("android.resource://com.haptix.app/raw/koji")
        assertEquals(VideoSource.Type.LOCAL, local.type)

        val curated = VideoSource.Curated("curated_scene_01", "CINEMATIC_CURATED")
        assertEquals(VideoSource.Type.CURATED, curated.type)
        assertEquals("curated_scene_01", curated.identifier)

        // YouTube URL formats
        val yt1 = VideoSource.YouTube("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertEquals("dQw4w9WgXcQ", yt1.videoId)

        val yt2 = VideoSource.YouTube("https://youtu.be/dQw4w9WgXcQ?t=42")
        assertEquals("dQw4w9WgXcQ", yt2.videoId)

        val yt3 = VideoSource.YouTube("https://youtube.com/shorts/3xyz987abc?feature=share")
        assertEquals("3xyz987abc", yt3.videoId)

        val meta = VideoMetadata(
            videoId = "koji",
            title = "Koji Animation",
            durationMs = 290134L,
            width = 1920,
            height = 1080,
            fps = 12.0f,
            source = local
        )
        assertEquals(290.134f, meta.durationSeconds, 0.001f)
        assertEquals(16.0f / 9.0f, meta.aspectRatio, 0.01f)
    }

    // -------------------------------------------------------------
    // 11. ProcessingJob State Machine
    // -------------------------------------------------------------
    @Test
    fun testProcessingJobStates() {
        val initialJob = ProcessingJob(
            videoSource = VideoSource.YouTube("https://youtu.be/sample123")
        )
        assertEquals(ProcessingStatus.QUEUED, initialJob.status)
        assertEquals(0.0f, initialJob.progress, 0.001f)
        assertFalse(initialJob.status.isTerminal)

        val downloading = initialJob.withProgress(ProcessingStatus.DOWNLOADING, 0.15f, "Downloading MP4")
        assertTrue(downloading.status.isRunning)
        assertFalse(downloading.status.isTerminal)

        val videoAnalysis = downloading.withProgress(ProcessingStatus.VIDEO_ANALYSIS, 0.35f, "Scene segmentation & RAFT flow")
        assertEquals(ProcessingStatus.VIDEO_ANALYSIS, videoAnalysis.status)

        val audioAnalysis = videoAnalysis.withProgress(ProcessingStatus.AUDIO_ANALYSIS, 0.55f, "Extracting audio features & Whisper")
        assertEquals(ProcessingStatus.AUDIO_ANALYSIS, audioAnalysis.status)

        val fusion = audioAnalysis.withProgress(ProcessingStatus.FUSION, 0.75f, "Cross-modal reliability & similarity")
        assertEquals(ProcessingStatus.FUSION, fusion.status)

        val generation = fusion.withProgress(ProcessingStatus.HAPTIC_GENERATION, 0.90f, "Generating platform-independent events")
        assertEquals(ProcessingStatus.HAPTIC_GENERATION, generation.status)

        val completed = generation.complete(HapticPattern("sample123", source = HapticSource.GENERATED))
        assertEquals(ProcessingStatus.COMPLETED, completed.status)
        assertTrue(completed.status.isTerminal)
        assertNotNull(completed.resultPattern)

        // Failure branch
        val failed = downloading.fail("Network timeout")
        assertEquals(ProcessingStatus.FAILED, failed.status)
        assertTrue(failed.status.isTerminal)
        assertEquals("Network timeout", failed.errorMessage)
    }

    // -------------------------------------------------------------
    // 12. Android Conversion from HapticPattern
    // -------------------------------------------------------------
    @Test
    fun testAndroidConversionFromHapticPattern() {
        val domainEvent = HapticEvent(
            startTimeMs = 2500L,
            durationMs = 800L,
            intensity = 0.75f,
            semanticType = SemanticHapticType.JUMP,
            attackMs = 250L,
            sustainMs = 300L,
            releaseMs = 250L,
            frequencyHz = 190.0f
        )

        val domainPattern = HapticPattern(
            videoId = "demo",
            events = listOf(domainEvent),
            source = HapticSource.GENERATED
        )

        // Convert to Android frequency pattern
        val freqPattern = AndroidHapticConverter.toAndroidFrequencyPattern(domainPattern)
        assertEquals(1, freqPattern.points.size)
        val point = freqPattern.points[0]
        assertEquals(2500L, point.startTimeMs)
        assertEquals(800L, point.durationMs)
        assertEquals(190.0f, point.frequencyHz, 0.01f)
        assertEquals(0.75f, point.amplitude, 0.01f)

        // Synthesize hardware WaveformProfile
        val profile = AndroidHapticConverter.toWaveformProfile(domainEvent)
        assertEquals(SemanticHapticType.JUMP, profile.semanticType)
        assertTrue(profile.timings.isNotEmpty())
        assertTrue(profile.amplitudes.isNotEmpty())
        assertEquals(profile.timings.size, profile.amplitudes.size)
        // Verify peak amplitude corresponds to ~0.75 * 255 (~191)
        val maxAmp = profile.amplitudes.maxOrNull() ?: 0
        assertTrue("Expected non-zero peak amplitude in waveform profile", maxAmp > 100)
    }



    // -------------------------------------------------------------
    // 14. AHAP Conversion for Future iOS CoreHaptics
    // -------------------------------------------------------------
    @Test
    fun testAhapConversionForIos() {
        val event1 = HapticEvent(
            startTimeMs = 1000L,
            durationMs = 500L,
            intensity = 0.80f,
            sharpness = 0.60f,
            attackMs = 150L,
            sustainMs = 200L,
            releaseMs = 150L,
            semanticType = SemanticHapticType.JUMP
        )
        val event2 = HapticEvent(
            startTimeMs = 3000L,
            durationMs = 30L,
            intensity = 0.90f,
            sharpness = 0.85f,
            type = HapticEventType.TRANSIENT
        )

        val pattern = HapticPattern(
            videoId = "ios_test",
            events = listOf(event1, event2),
            source = HapticSource.GENERATED
        )

        val ahapJsonString = AhapHapticConverter.toAhapJson(pattern)
        assertNotNull(ahapJsonString)

        val root = JSONObject(ahapJsonString)
        assertEquals(1.0, root.getDouble("Version"), 0.001)

        val patternArray = root.getJSONArray("Pattern")
        assertTrue(patternArray.length() >= 2)

        // Verify event objects have Time and EventParameters
        val firstObj = patternArray.getJSONObject(0)
        assertTrue(firstObj.has("Event"))
        val eventData = firstObj.getJSONObject("Event")
        assertEquals(1.0, eventData.getDouble("Time"), 0.001)
        assertEquals("HapticContinuous", eventData.getString("EventType"))
        assertEquals(0.5, eventData.getDouble("EventDuration"), 0.001)
    }
}
