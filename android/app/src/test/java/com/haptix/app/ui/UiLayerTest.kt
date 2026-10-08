package com.haptix.app.ui

import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.model.Participant
import com.haptix.app.data.repository.DefaultHapticRepository
import com.haptix.app.data.repository.DefaultVideoRepository
import com.haptix.app.data.repository.InMemoryParticipantRepository
import com.haptix.app.haptics.HapticEngine
import com.haptix.app.haptics.HapticSynchronizer
import com.haptix.app.monitoring.PerformanceMonitor
import com.haptix.app.session.StudySessionViewModel
import com.haptix.app.R
import com.haptix.app.ui.components.formatDuration
import com.haptix.app.ui.components.resolveThumbnailDrawable
import com.haptix.app.ui.screens.feedback.FeedbackViewModel
import com.haptix.app.ui.screens.feedback.STUDY_QUESTIONS
import com.haptix.app.ui.screens.profile.ProfileViewModel
import com.haptix.app.ui.screens.video.VideoListViewModel
import com.haptix.app.ui.screens.video.VideoPlayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UiLayerTest {

    // Helper fake HapticEngine for testing
    private class FakeHapticEngine(private val supported: Boolean = true) : HapticEngine {
        var isPlaying: Boolean = false
        var loadedPattern: HapticPattern? = null
        val playedEvents = mutableListOf<HapticEvent>()

        override fun isHapticSupported(): Boolean = supported
        override fun loadPattern(pattern: HapticPattern) { loadedPattern = pattern }
        override fun start() { isPlaying = true }
        override fun stop() { isPlaying = false }
        override fun release() { isPlaying = false; loadedPattern = null }
        override fun playEvent(event: HapticEvent) { playedEvents.add(event) }
    }

    // 1. Duration formatting test
    @Test
    fun formatDuration_formatsCorrectly() {
        assertEquals("00:00", formatDuration(0L))
        assertEquals("00:30", formatDuration(30_000L))
        assertEquals("01:05", formatDuration(65_000L))
        assertEquals("10:00", formatDuration(600_000L))
    }

    // 2. ProfileViewModel validation tests
    @Test
    fun profileViewModel_validatesSensibleAgeRanges() {
        val viewModel = ProfileViewModel()

        // Empty age -> invalid
        viewModel.updateAge("")
        assertEquals("Age is required", viewModel.uiState.value.ageError)
        assertFalse(viewModel.uiState.value.isValid)

        // Under 18 -> invalid
        viewModel.updateAge("17")
        assertEquals("Participant must be at least 18 years old", viewModel.uiState.value.ageError)
        assertFalse(viewModel.uiState.value.isValid)

        // Above 100 -> invalid
        viewModel.updateAge("101")
        assertEquals("Please enter a valid age (up to 100)", viewModel.uiState.value.ageError)
        assertFalse(viewModel.uiState.value.isValid)

        // Valid age 25
        viewModel.updateAge("25")
        assertNull(viewModel.uiState.value.ageError)
        assertEquals("25", viewModel.uiState.value.ageText)

        // Set remaining required fields
        viewModel.updateGender("Female")
        viewModel.updateFavoriteGenre("Sci-Fi")
        viewModel.updateMovieInterest(4)

        assertTrue(viewModel.uiState.value.isValid)
    }

    @Test
    fun profileViewModel_savesParticipantCorrectly() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val repo = InMemoryParticipantRepository()
        val viewModel = ProfileViewModel(participantRepository = repo, coroutineScope = testScope)

        viewModel.updateParticipantId("P-TEST-1")
        viewModel.updateGender("Male")
        viewModel.updateAge("30")
        viewModel.updateMovieInterest(5)
        viewModel.updateFavoriteGenre("Drama")

        var savedParticipant: Participant? = null
        viewModel.saveParticipant { participant ->
            savedParticipant = participant
        }

        assertNotNull(savedParticipant)
        assertEquals("P-TEST-1", savedParticipant?.id)
        assertEquals("Male", savedParticipant?.gender)
        assertEquals(30, savedParticipant?.age)
        assertEquals(5, savedParticipant?.movieInterestRating)
        assertEquals("Drama", savedParticipant?.favoriteGenre)
    }

    // 3. VideoListViewModel tests
    @Test
    fun videoListViewModel_loadsStimuliList() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val repo = DefaultVideoRepository()
        val viewModel = VideoListViewModel(videoRepository = repo, coroutineScope = testScope)

        viewModel.selectVideo("video_02")
        assertEquals("video_02", viewModel.uiState.value.selectedVideoId)
    }

    // 4. VideoPlayerViewModel tests
    @Test
    fun videoPlayerViewModel_controlsPlaybackAndSeeking() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val fakeEngine = FakeHapticEngine(supported = true)
        val synchronizer = HapticSynchronizer(engine = fakeEngine)
        val viewModel = VideoPlayerViewModel(
            videoRepository = DefaultVideoRepository(),
            hapticRepository = DefaultHapticRepository(),
            hapticSynchronizer = synchronizer,
            performanceMonitor = PerformanceMonitor(),
            hapticEngine = fakeEngine,
            coroutineScope = testScope
        )

        // Load video
        viewModel.loadVideo("video_01", sessionHapticsEnabled = true)

        // Seeking updates position and notifies synchronizer
        viewModel.seekTo(12_000L)
        assertEquals(12_000L, viewModel.uiState.value.currentPositionMs)

        // Seeking clamps to duration bounds
        viewModel.seekTo(-500L)
        assertEquals(0L, viewModel.uiState.value.currentPositionMs)

        // Volume toggle
        assertFalse(viewModel.uiState.value.isMuted)
        viewModel.toggleVolume()
        assertTrue(viewModel.uiState.value.isMuted)

        // Fullscreen toggle
        assertFalse(viewModel.uiState.value.isFullscreen)
        viewModel.toggleFullscreen()
        assertTrue(viewModel.uiState.value.isFullscreen)
    }

    @Test
    fun videoPlayerViewModel_handlesUnsupportedHapticHardwareGracefully() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val unsupportedEngine = FakeHapticEngine(supported = false)
        val synchronizer = HapticSynchronizer(engine = unsupportedEngine)
        val viewModel = VideoPlayerViewModel(
            videoRepository = DefaultVideoRepository(),
            hapticRepository = DefaultHapticRepository(),
            hapticSynchronizer = synchronizer,
            performanceMonitor = PerformanceMonitor(),
            hapticEngine = unsupportedEngine,
            coroutineScope = testScope
        )

        assertFalse(viewModel.uiState.value.isHapticSupported)
        assertFalse(viewModel.uiState.value.isHapticsEnabled)

        // Toggling haptics when unsupported does not enable it
        viewModel.toggleHaptics()
        assertFalse(viewModel.uiState.value.isHapticsEnabled)
    }

    @Test
    fun videoPlayerViewModel_handlesRealPlaybackStateTransitions() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = VideoPlayerViewModel(
            videoRepository = DefaultVideoRepository(),
            coroutineScope = testScope
        )

        viewModel.loadVideo("video_01")
        assertFalse(viewModel.uiState.value.isPlaying)
        assertFalse(viewModel.uiState.value.isCompleted)

        // State transition: Playing
        viewModel.updatePlaybackState(isPlaying = true, isCompleted = false)
        assertTrue(viewModel.uiState.value.isPlaying)
        assertFalse(viewModel.uiState.value.isCompleted)

        // State transition: Paused
        viewModel.updatePlaybackState(isPlaying = false, isCompleted = false)
        assertFalse(viewModel.uiState.value.isPlaying)
        assertFalse(viewModel.uiState.value.isCompleted)

        // State transition: Ended / Completed
        viewModel.updatePlaybackState(isPlaying = false, isCompleted = true)
        assertFalse(viewModel.uiState.value.isPlaying)
        assertTrue(viewModel.uiState.value.isCompleted)
    }

    @Test
    fun videoPlayerViewModel_handlesDurationAndPositionRestoration() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = VideoPlayerViewModel(
            videoRepository = DefaultVideoRepository(),
            coroutineScope = testScope
        )

        viewModel.loadVideo("video_01")

        // Duration update from media metadata
        viewModel.updateDuration(42_500L)
        assertEquals(42_500L, viewModel.uiState.value.durationMs)

        // Seek clamps to updated duration
        viewModel.seekTo(50_000L)
        assertEquals(42_500L, viewModel.uiState.value.currentPositionMs)
        assertTrue(viewModel.uiState.value.isCompleted)

        // Lifecycle save and restoration of position
        viewModel.savePlaybackPosition(15_300L)
        assertEquals(15_300L, viewModel.getSavedPlaybackPosition())

        // Position updates dispatch to state
        viewModel.onTimelinePositionChanged(20_000L)
        assertEquals(20_000L, viewModel.uiState.value.currentPositionMs)
        assertEquals(20_000L, viewModel.getSavedPlaybackPosition())
    }

    @Test
    fun videoPlayerViewModel_controlsVisibilityToggleSequence() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = VideoPlayerViewModel(coroutineScope = testScope)
        viewModel.loadVideo("video_01")

        // 1. Initial State: Controls are visible
        assertTrue(viewModel.uiState.value.controlsVisible)

        // 2. Tap 1: VISIBLE -> HIDDEN
        viewModel.toggleControlsVisibility()
        assertFalse(viewModel.uiState.value.controlsVisible)

        // 3. Tap 2: HIDDEN -> VISIBLE
        viewModel.toggleControlsVisibility()
        assertTrue(viewModel.uiState.value.controlsVisible)

        // 4. Tap 3: VISIBLE -> HIDDEN
        viewModel.toggleControlsVisibility()
        assertFalse(viewModel.uiState.value.controlsVisible)

        // 5. Tap 4: HIDDEN -> VISIBLE
        viewModel.toggleControlsVisibility()
        assertTrue(viewModel.uiState.value.controlsVisible)

        // Repeat sequence to verify indefinite toggle reliability (5+ cycles)
        for (i in 1..5) {
            // VISIBLE -> HIDDEN
            viewModel.toggleControlsVisibility()
            assertFalse("Expected controls hidden on cycle $i toggle 1", viewModel.uiState.value.controlsVisible)

            // HIDDEN -> VISIBLE
            viewModel.toggleControlsVisibility()
            assertTrue("Expected controls visible on cycle $i toggle 2", viewModel.uiState.value.controlsVisible)
        }

        // Test explicit setControlsVisibility
        viewModel.setControlsVisibility(false)
        assertFalse(viewModel.uiState.value.controlsVisible)

        viewModel.setControlsVisibility(true)
        assertTrue(viewModel.uiState.value.controlsVisible)
    }

    @Test
    fun videoPlayerViewModel_landscapeAndPlaybackControlsToggle() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)
        val viewModel = VideoPlayerViewModel(coroutineScope = testScope)
        viewModel.loadVideo("video_01")

        // Enter landscape / fullscreen
        viewModel.toggleFullscreen()
        assertTrue(viewModel.uiState.value.isFullscreen)
        assertTrue(viewModel.uiState.value.controlsVisible)

        // Tap to hide controls in landscape
        viewModel.toggleControlsVisibility()
        assertFalse(viewModel.uiState.value.controlsVisible)

        // Start playback while controls are hidden: playback state updates without breaking toggle state
        viewModel.updatePlaybackState(isPlaying = true, isCompleted = false)
        assertTrue(viewModel.uiState.value.isPlaying)
        assertFalse(viewModel.uiState.value.controlsVisible)

        // Tap to show controls during playback
        viewModel.toggleControlsVisibility()
        assertTrue(viewModel.uiState.value.controlsVisible)

        // Seek while controls are visible
        viewModel.seekTo(5000L)
        assertEquals(5000L, viewModel.uiState.value.currentPositionMs)
        assertTrue(viewModel.uiState.value.controlsVisible)

        // Tap to hide controls again
        viewModel.toggleControlsVisibility()
        assertFalse(viewModel.uiState.value.controlsVisible)

        // Tap to show controls
        viewModel.toggleControlsVisibility()
        assertTrue(viewModel.uiState.value.controlsVisible)

        // Exit fullscreen to portrait
        viewModel.toggleFullscreen()
        assertFalse(viewModel.uiState.value.isFullscreen)
        assertTrue(viewModel.uiState.value.controlsVisible)
    }

    // 5. FeedbackViewModel tests
    @Test
    fun feedbackViewModel_has5FixedQuestionsAndClampsRatings() {
        val viewModel = FeedbackViewModel()
        viewModel.setVideoId("video_01")

        assertEquals(5, STUDY_QUESTIONS.size)

        // Rating clamping (1..5)
        viewModel.updateRating(1, 0)
        assertEquals(1, viewModel.getRatingForQuestion(1))

        viewModel.updateRating(1, 6)
        assertEquals(5, viewModel.getRatingForQuestion(1))

        viewModel.updateRating(2, 4)
        viewModel.updateRating(3, 5)
        viewModel.updateRating(4, 2)
        viewModel.updateRating(5, 5)

        val responses = viewModel.buildResponses(participantId = "p_101", videoId = "video_01")
        assertEquals(5, responses.size)
        assertEquals("q_visual", responses[0].questionId)
        assertEquals(5, responses[0].rating)
        assertEquals("q_immersion", responses[1].questionId)
        assertEquals(4, responses[1].rating)
        assertEquals("q_synchrony", responses[2].questionId)
        assertEquals(5, responses[2].rating)
        assertEquals("q_realism", responses[3].questionId)
        assertEquals(2, responses[3].rating)
        assertEquals("q_satisfaction", responses[4].questionId)
        assertEquals(5, responses[4].rating)

        // All responses contain correct participant and video identifiers
        responses.forEach { response ->
            assertEquals("p_101", response.participantId)
            assertEquals("video_01", response.videoId)
        }
    }

    // 6. StudySessionViewModel tests
    @Test
    fun studySessionViewModel_persistsSessionAndResets() {
        val sessionViewModel = StudySessionViewModel(initialHapticSupported = true)

        assertTrue(sessionViewModel.sessionState.value.isHapticSupported)
        assertTrue(sessionViewModel.sessionState.value.isHapticsEnabled)

        // Toggle haptics
        sessionViewModel.setHapticsEnabled(false)
        assertFalse(sessionViewModel.sessionState.value.isHapticsEnabled)

        // Save participant
        val participant = Participant(
            id = "P-101",
            gender = "Female",
            age = 28,
            movieInterestRating = 4,
            favoriteGenre = "Sci-Fi"
        )
        sessionViewModel.saveParticipant(participant)
        assertEquals(participant, sessionViewModel.sessionState.value.participant)

        // Select video
        sessionViewModel.selectVideo("video_03")
        assertEquals("video_03", sessionViewModel.sessionState.value.selectedVideoId)

        // Reset session
        sessionViewModel.resetSession()
        assertNull(sessionViewModel.sessionState.value.participant)
        assertNull(sessionViewModel.sessionState.value.selectedVideoId)
        assertTrue(sessionViewModel.sessionState.value.isHapticsEnabled)
    }

    // 7. Thumbnail integration tests for F1 and Koji
    @Test
    fun thumbnailIntegration_f1AndKojiResolveRealDrawables() = runBlocking {
        val repo = DefaultVideoRepository()
        val videos = repo.getVideos().first()

        val f1 = videos.find { it.id == "f1_2025_haptic_trailer" }
        assertNotNull("F1 video must exist", f1)
        assertEquals("f1_2025_thumbnail", f1?.thumbnailResUri)
        assertEquals(R.drawable.f1_2025_thumbnail, f1?.thumbnailResId)
        assertEquals(R.drawable.f1_2025_thumbnail, resolveThumbnailDrawable(f1!!))

        val koji = videos.find { it.id == "koji" }
        assertNotNull("Koji video must exist", koji)
        assertEquals("koji_thumbnail", koji?.thumbnailResUri)
        assertEquals(R.drawable.koji_thumbnail, koji?.thumbnailResId)
        assertEquals(R.drawable.koji_thumbnail, resolveThumbnailDrawable(koji!!))
    }
}
