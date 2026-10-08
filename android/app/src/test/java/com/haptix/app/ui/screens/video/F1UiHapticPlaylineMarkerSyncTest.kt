package com.haptix.app.ui.screens.video

import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.parser.FrequencyPatternParser
import com.haptix.app.data.repository.DefaultHapticRepository
import com.haptix.app.data.repository.DefaultVideoRepository
import com.haptix.app.domain.provider.CuratedHapticProvider
import com.haptix.app.haptics.HapticEngine
import com.haptix.app.haptics.HapticSynchronizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class F1UiHapticPlaylineMarkerSyncTest {

    private class RecordingHapticEngine : HapticEngine {
        val playedEvents = mutableListOf<HapticEvent>()
        override fun isHapticSupported(): Boolean = true
        override fun loadPattern(pattern: HapticPattern) {}
        override fun start() {}
        override fun stop() {}
        override fun release() {}
        override fun playEvent(event: HapticEvent) {
            playedEvents.add(event)
        }
    }

    private fun findAssetFile(path: String): File {
        val candidates = listOf(
            File("src/main/assets/$path"),
            File("app/src/main/assets/$path"),
            File("android/app/src/main/assets/$path"),
            File("../app/src/main/assets/$path")
        )
        return candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("Asset not found in candidates: $path")
    }

    private lateinit var f1Json: String
    private lateinit var parsedPattern: HapticPattern

    @Before
    fun setUp() {
        val assetFile = findAssetFile("haptics/f1_haptic_timeline.json")
        f1Json = assetFile.readText()

        val parseResult = FrequencyPatternParser.parseToHapticPattern(f1Json)
        assertTrue("Parsing f1_haptic_timeline.json failed: ${parseResult.exceptionOrNull()?.message}", parseResult.isSuccess)
        parsedPattern = parseResult.getOrThrow()
    }

    @Test
    fun f1TimelineJson_parsesAuthoritativeEventTimestamps() {
        assertEquals("f1_2025_haptic_trailer", parsedPattern.videoId)
        assertEquals(141, parsedPattern.events.size)

        // Event 1 (f1_001) authoritative timestamps
        val firstEvent = parsedPattern.events.first()
        assertEquals("f1_001", firstEvent.id)
        assertEquals(1013L, firstEvent.startTimeMs)
        assertEquals(1141L, firstEvent.peakTimeMs)
        assertEquals(1141L, firstEvent.effectivePeakTimeMs)
        assertEquals(235L, firstEvent.durationMs)
        assertEquals(1248L, firstEvent.endTimeMs)

        // Event 2 (f1_002) authoritative timestamps
        val secondEvent = parsedPattern.events[1]
        assertEquals("f1_002", secondEvent.id)
        assertEquals(1248L, secondEvent.startTimeMs)
        assertEquals(1419L, secondEvent.peakTimeMs)
        assertEquals(437L, secondEvent.durationMs)

        // Landmark event around 26.6s (f1_017)
        val landmark017 = parsedPattern.events.firstOrNull { it.id == "f1_017" }
        assertNotNull(landmark017)
        assertEquals(26613L, landmark017!!.startTimeMs)
        assertEquals(26624L, landmark017.peakTimeMs)

        // Landmark event around 27.1s (f1_020)
        val landmark020 = parsedPattern.events.firstOrNull { it.id == "f1_020" }
        assertNotNull(landmark020)
        assertEquals(27115L, landmark020!!.startTimeMs)
        assertEquals(27125L, landmark020.peakTimeMs)
    }

    @Test
    fun f1TimelineJson_validatesPerceptualSilenceWindowsAndBoundaryInvariants() {
        val events = parsedPattern.events

        // 1. Verify expected event count = 141
        assertEquals(141, events.size)

        // 2. Validate no event overlaps 37.000s through 42.000s
        val overlap1 = events.filter { !(it.endTimeMs <= 37000L || it.startTimeMs >= 42000L) }
        assertTrue("No event must overlap 37.000s-42.000s, found: ${overlap1.map { it.id }}", overlap1.isEmpty())

        // 3. Validate no event overlaps 52.000s through 68.000s
        val overlap2 = events.filter { !(it.endTimeMs <= 52000L || it.startTimeMs >= 68000L) }
        assertTrue("No event must overlap 52.000s-68.000s, found: ${overlap2.map { it.id }}", overlap2.isEmpty())

        // 4. Validate no event overlaps 85.000s through 88.000s
        val overlap3 = events.filter { !(it.endTimeMs <= 85000L || it.startTimeMs >= 88000L) }
        assertTrue("No event must overlap 85.000s-88.000s, found: ${overlap3.map { it.id }}", overlap3.isEmpty())

        // 5. Validate removed events are absent
        val removedIds = listOf(
            "f1_047", "f1_048",
            "f1_067", "f1_068", "f1_069", "f1_070", "f1_071", "f1_072",
            "f1_073", "f1_074", "f1_075", "f1_076", "f1_077", "f1_078",
            "f1_079", "f1_080", "f1_081", "f1_082", "f1_083", "f1_084",
            "f1_085", "f1_086", "f1_087", "f1_088", "f1_089", "f1_090",
            "f1_115", "f1_116", "f1_117", "f1_118", "f1_119", "f1_120"
        )
        for (id in removedIds) {
            assertTrue("Removed event $id must not exist", events.none { it.id == id })
        }

        // 6. Validate key boundary events remain unchanged
        val e49 = events.firstOrNull { it.id == "f1_049" }
        assertNotNull("f1_049 must remain", e49)
        assertEquals(42581L, e49!!.startTimeMs)

        val e50 = events.firstOrNull { it.id == "f1_050" }
        assertNotNull("f1_050 must remain", e50)
        assertEquals(42837L, e50!!.startTimeMs)

        val e91 = events.firstOrNull { it.id == "f1_091" }
        assertNotNull("f1_091 must remain", e91)
        assertEquals(68416L, e91!!.startTimeMs)

        val e114 = events.firstOrNull { it.id == "f1_114" }
        assertNotNull("f1_114 must remain", e114)
        assertEquals(84341L, e114!!.startTimeMs)

        val e121 = events.firstOrNull { it.id == "f1_121" }
        assertNotNull("f1_121 must remain", e121)
        assertEquals(88277L, e121!!.startTimeMs)

        // 7. Validate strictly sorted and no overlap
        for (i in 0 until events.size - 1) {
            val curr = events[i]
            val next = events[i + 1]
            assertTrue("Events must be strictly sorted: ${curr.id} (${curr.startTimeMs}) >= ${next.id} (${next.startTimeMs})", curr.startTimeMs < next.startTimeMs)
            assertTrue("Events must not overlap: ${curr.id} (${curr.endTimeMs}) > ${next.id} (${next.startTimeMs})", curr.endTimeMs <= next.startTimeMs)
        }
    }

    @Test
    fun videoPlayerViewModel_synchronizesHapticPatternToUiMarkersAndSynchronizer() = runBlocking {
        val videoRepo = DefaultVideoRepository()
        val engine = RecordingHapticEngine()
        val synchronizer = HapticSynchronizer(engine = engine)
        val testScope = CoroutineScope(Dispatchers.Unconfined)

        val curatedProvider = CuratedHapticProvider(assetReader = { path ->
            try {
                findAssetFile(path.removePrefix("haptics/")).readText()
            } catch (_: Exception) {
                try {
                    findAssetFile(path).readText()
                } catch (_: Exception) {
                    null
                }
            }
        })
        val hapticRepo = DefaultHapticRepository(providers = listOf(curatedProvider))

        val viewModel = VideoPlayerViewModel(
            videoRepository = videoRepo,
            hapticRepository = hapticRepo,
            hapticSynchronizer = synchronizer,
            hapticEngine = engine,
            coroutineScope = testScope
        )

        // Load the F1 video stimulus
        viewModel.loadVideo("f1_2025_haptic_trailer")

        val uiState = viewModel.uiState.value

        // Verify UI markers receive the authoritative parsed events (141 after perceptual corrections)
        assertEquals(141, uiState.hapticEvents.size)
        assertEquals(141, uiState.hapticEventCount)
        assertNotNull(uiState.hapticPattern)

        // Verify UI playline markers do not contain any events in the silence intervals
        val uiOverlap1 = uiState.hapticEvents.filter { !(it.endTimeMs <= 37000L || it.startTimeMs >= 42000L) }
        assertTrue("UI markers must have no events in 37.000s-42.000s", uiOverlap1.isEmpty())

        val uiOverlap2 = uiState.hapticEvents.filter { !(it.endTimeMs <= 52000L || it.startTimeMs >= 68000L) }
        assertTrue("UI markers must have no events in 52.000s-68.000s", uiOverlap2.isEmpty())

        val uiOverlap3 = uiState.hapticEvents.filter { !(it.endTimeMs <= 85000L || it.startTimeMs >= 88000L) }
        assertTrue("UI markers must have no events in 85.000s-88.000s", uiOverlap3.isEmpty())

        // Verify UI playline marker timestamps match f1_haptic_timeline.json exactly
        val firstUiMarker = uiState.hapticEvents.first()
        assertEquals(1013L, firstUiMarker.startTimeMs)
        assertEquals(1141L, firstUiMarker.peakTimeMs)
        assertEquals(235L, firstUiMarker.durationMs)

        // Verify HapticSynchronizer received the identical HapticPattern
        val synPattern = synchronizer.getPattern()
        assertNotNull(synPattern)
        assertEquals(141, synPattern!!.events.size)
        assertEquals(uiState.hapticEvents, synPattern.events)

        // Verify timeline position dispatch synchronizes with the same events
        viewModel.onTimelinePositionChanged(1141L)
        // Engine should have received actuation event near 1141ms
        assertTrue("Haptic engine should actuate event near 1141ms", engine.playedEvents.isNotEmpty())
        assertEquals(1013L, engine.playedEvents.first().startTimeMs)
    }

    @Test
    fun playlineMarkerTimestamp_projectionAcrossTimelineWidth() {
        val maxDuration = 130549L
        val testWidth = 1000f

        val firstEvent = parsedPattern.events.first()
        val expectedStartX = (firstEvent.startTimeMs.toFloat() / maxDuration) * testWidth
        val expectedPeakX = ((firstEvent.effectivePeakTimeMs).toFloat() / maxDuration) * testWidth

        assertTrue("Start X should be within bounds", expectedStartX in 0f..testWidth)
        assertTrue("Peak X should be after Start X", expectedPeakX >= expectedStartX)
        assertEquals(7.759f, expectedStartX, 0.01f)
        assertEquals(8.739f, expectedPeakX, 0.01f)
    }
}
