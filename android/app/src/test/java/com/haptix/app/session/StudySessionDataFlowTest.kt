package com.haptix.app.session

import com.haptix.app.data.model.FeedbackResponse
import com.haptix.app.data.model.Participant
import com.haptix.app.data.model.PerformanceMetric
import com.haptix.app.ui.screens.feedback.FeedbackViewModel
import com.haptix.app.ui.screens.profile.ProfileUiState
import com.haptix.app.ui.screens.profile.ProfileViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudySessionDataFlowTest {

    // =========================================================================
    // 1. Participant Profile Validation & Persistence
    // =========================================================================

    @Test
    fun participantProfile_validationEnforcesRequiredFields() {
        val validState = ProfileUiState(
            participantId = "P-101",
            gender = "Non-binary",
            ageText = "24",
            movieInterestRating = 4,
            favoriteGenre = "Sci-Fi"
        )
        assertTrue("All required fields provided and valid", validState.isValid)

        val invalidUnderageState = validState.copy(ageText = "16")
        assertFalse("Underage participant (< 18) must be invalid", invalidUnderageState.isValid)

        val invalidOverageState = validState.copy(ageText = "105")
        assertFalse("Age > 100 must be invalid", invalidOverageState.isValid)

        val invalidMissingGenderState = validState.copy(gender = "")
        assertFalse("Missing gender must be invalid", invalidMissingGenderState.isValid)

        val invalidMissingGenreState = validState.copy(favoriteGenre = "")
        assertFalse("Missing favorite genre must be invalid", invalidMissingGenreState.isValid)

        val invalidBlankAgeState = validState.copy(ageText = "")
        assertFalse("Blank age must be invalid", invalidBlankAgeState.isValid)
    }

    @Test
    fun participantProfile_savedToStudySessionState() {
        val sessionViewModel = StudySessionViewModel(initialHapticSupported = true)
        val profileViewModel = ProfileViewModel(coroutineScope = CoroutineScope(Dispatchers.Unconfined))

        profileViewModel.updateParticipantId("P-404")
        profileViewModel.updateGender("Female")
        profileViewModel.updateAge("29")
        profileViewModel.updateMovieInterest(5)
        profileViewModel.updateFavoriteGenre("Action")

        assertTrue(profileViewModel.uiState.value.isValid)

        profileViewModel.saveParticipant { participant ->
            sessionViewModel.saveParticipant(participant)
        }

        val saved = sessionViewModel.sessionState.value.participant
        assertNotNull("Participant must be stored in session state", saved)
        assertEquals("P-404", saved!!.id)
        assertEquals("Female", saved.gender)
        assertEquals(29, saved.age)
        assertEquals(5, saved.movieInterestRating)
        assertEquals("Action", saved.favoriteGenre)
    }

    // =========================================================================
    // 2. Stimulus Selection & Context Isolation
    // =========================================================================

    @Test
    fun stimulusSelection_preservesParticipantProfileAndSetsStimulusId() {
        val sessionViewModel = StudySessionViewModel()
        val participant = Participant(
            id = "P-202",
            gender = "Male",
            age = 31,
            movieInterestRating = 3,
            favoriteGenre = "Drama"
        )
        sessionViewModel.saveParticipant(participant)

        sessionViewModel.selectVideo("video_f1_trailer")
        assertEquals("video_f1_trailer", sessionViewModel.sessionState.value.selectedVideoId)
        assertEquals(participant, sessionViewModel.sessionState.value.participant)

        // Switching stimulus retains the participant profile
        sessionViewModel.selectVideo("video_koji")
        assertEquals("video_koji", sessionViewModel.sessionState.value.selectedVideoId)
        assertEquals(participant, sessionViewModel.sessionState.value.participant)
    }

    // =========================================================================
    // 3. Performance Metrics Association & Playback Position
    // =========================================================================

    @Test
    fun performanceMetrics_retainsStimulusAssociationAndPlaybackPosition() {
        val sessionViewModel = StudySessionViewModel()

        val metricsStimulusA = listOf(
            PerformanceMetric(
                timestamp = 1000L,
                cpuUsagePercent = 25.0f,
                gpuUsagePercent = null,
                deviceTemperatureCelsius = 35.0f,
                videoId = "stimulus_A",
                playbackPositionMs = 2000L
            ),
            PerformanceMetric(
                timestamp = 3000L,
                cpuUsagePercent = 28.0f,
                gpuUsagePercent = null,
                deviceTemperatureCelsius = 35.2f,
                videoId = "stimulus_A",
                playbackPositionMs = 4000L
            )
        )

        sessionViewModel.recordPerformanceMetrics(metricsStimulusA)

        val stored = sessionViewModel.sessionState.value.performanceMetrics
        assertEquals(2, stored.size)
        stored.forEach {
            assertEquals("stimulus_A", it.videoId)
            assertNotNull(it.playbackPositionMs)
        }
    }

    // =========================================================================
    // 4. Feedback Validation, 1-5 Scale & Duplicate Prevention
    // =========================================================================

    @Test
    fun feedbackViewModel_clampsRatingsAndBuildsStandardizedResponses() {
        val feedbackViewModel = FeedbackViewModel()
        feedbackViewModel.setVideoId("stimulus_A")

        // Rating clamping (1..5)
        feedbackViewModel.updateRating(1, -5)
        assertEquals(1, feedbackViewModel.getRatingForQuestion(1))

        feedbackViewModel.updateRating(2, 10)
        assertEquals(5, feedbackViewModel.getRatingForQuestion(2))

        feedbackViewModel.updateRating(3, 4)
        feedbackViewModel.updateRating(4, 3)
        feedbackViewModel.updateRating(5, 5)

        assertTrue(feedbackViewModel.areAllQuestionsAnswered())

        val responses = feedbackViewModel.buildResponses("P-101", "stimulus_A")
        assertEquals(5, responses.size)
        assertEquals("q_visual", responses[0].questionId)
        assertEquals(1, responses[0].rating)
        assertEquals("q_immersion", responses[1].questionId)
        assertEquals(5, responses[1].rating)
        assertEquals("q_synchrony", responses[2].questionId)
        assertEquals(4, responses[2].rating)
        assertEquals("q_realism", responses[3].questionId)
        assertEquals(3, responses[3].rating)
        assertEquals("q_satisfaction", responses[4].questionId)
        assertEquals(5, responses[4].rating)

        responses.forEach {
            assertEquals("P-101", it.participantId)
            assertEquals("stimulus_A", it.videoId)
            assertTrue("Rating must be in 1..5", it.rating in 1..5)
        }
    }

    @Test
    fun feedbackViewModel_preventsDuplicateSubmission() {
        val feedbackViewModel = FeedbackViewModel()
        feedbackViewModel.setVideoId("stimulus_A")

        var submissionCount = 0
        val onSubmitted: (List<FeedbackResponse>) -> Unit = { submissionCount++ }

        // First submission succeeds
        feedbackViewModel.submitFeedback(
            participantId = "P-101",
            hapticEnabled = true,
            onSubmitted = onSubmitted
        )
        assertEquals(1, submissionCount)
        assertTrue(feedbackViewModel.uiState.value.isSubmitted)

        // Attempt second submission: should be blocked
        feedbackViewModel.submitFeedback(
            participantId = "P-101",
            hapticEnabled = true,
            onSubmitted = onSubmitted
        )
        assertEquals("Duplicate submission must be prevented", 1, submissionCount)
    }

    @Test
    fun feedbackViewModel_rejectsSubmissionWithoutVideoId() {
        val feedbackViewModel = FeedbackViewModel()
        // VideoId not set (blank)
        var called = false
        feedbackViewModel.submitFeedback("P-101", true) { called = true }
        assertFalse("Submission without videoId must be rejected", called)
    }

    // =========================================================================
    // 5. Multiple Stimuli Separation & Preservation
    // =========================================================================

    @Test
    fun studySession_supportsMultipleStimuliWithoutDataBleed() {
        val sessionViewModel = StudySessionViewModel()
        val participant = Participant(id = "P-Multi", gender = "Female", age = 26, movieInterestRating = 4, favoriteGenre = "Animation")
        sessionViewModel.saveParticipant(participant)

        // Stimulus 1: F1
        sessionViewModel.selectVideo("stimulus_f1")
        val metricsF1 = listOf(
            PerformanceMetric(videoId = "stimulus_f1", cpuUsagePercent = 22.0f, playbackPositionMs = 1000L),
            PerformanceMetric(videoId = "stimulus_f1", cpuUsagePercent = 24.0f, playbackPositionMs = 3000L)
        )
        sessionViewModel.recordPerformanceMetrics(metricsF1)

        val feedbackF1 = listOf(
            FeedbackResponse(videoId = "stimulus_f1", questionId = "q_visual", rating = 5, participantId = "P-Multi"),
            FeedbackResponse(videoId = "stimulus_f1", questionId = "q_immersion", rating = 4, participantId = "P-Multi"),
            FeedbackResponse(videoId = "stimulus_f1", questionId = "q_synchrony", rating = 5, participantId = "P-Multi"),
            FeedbackResponse(videoId = "stimulus_f1", questionId = "q_realism", rating = 4, participantId = "P-Multi"),
            FeedbackResponse(videoId = "stimulus_f1", questionId = "q_satisfaction", rating = 5, participantId = "P-Multi")
        )
        sessionViewModel.recordFeedback(feedbackF1)

        // Stimulus 2: Koji
        sessionViewModel.selectVideo("stimulus_koji")
        val metricsKoji = listOf(
            PerformanceMetric(videoId = "stimulus_koji", cpuUsagePercent = 18.0f, playbackPositionMs = 1000L),
            PerformanceMetric(videoId = "stimulus_koji", cpuUsagePercent = 19.0f, playbackPositionMs = 3000L),
            PerformanceMetric(videoId = "stimulus_koji", cpuUsagePercent = 20.0f, playbackPositionMs = 5000L)
        )
        sessionViewModel.recordPerformanceMetrics(metricsKoji)

        val feedbackKoji = listOf(
            FeedbackResponse(videoId = "stimulus_koji", questionId = "q_visual", rating = 4, participantId = "P-Multi"),
            FeedbackResponse(videoId = "stimulus_koji", questionId = "q_immersion", rating = 3, participantId = "P-Multi"),
            FeedbackResponse(videoId = "stimulus_koji", questionId = "q_synchrony", rating = 4, participantId = "P-Multi"),
            FeedbackResponse(videoId = "stimulus_koji", questionId = "q_realism", rating = 3, participantId = "P-Multi"),
            FeedbackResponse(videoId = "stimulus_koji", questionId = "q_satisfaction", rating = 4, participantId = "P-Multi")
        )
        sessionViewModel.recordFeedback(feedbackKoji)

        val state = sessionViewModel.sessionState.value

        // Participant is still intact
        assertEquals(participant, state.participant)

        // Both stimuli are recognized
        assertEquals(listOf("stimulus_f1", "stimulus_koji"), state.evaluatedVideoIds)

        // Total metrics = 2 (F1) + 3 (Koji) = 5
        assertEquals(5, state.performanceMetrics.size)
        val f1Metrics = state.getMetricsForVideo("stimulus_f1")
        assertEquals(2, f1Metrics.size)
        assertTrue(f1Metrics.all { it.videoId == "stimulus_f1" })

        val kojiMetrics = state.getMetricsForVideo("stimulus_koji")
        assertEquals(3, kojiMetrics.size)
        assertTrue(kojiMetrics.all { it.videoId == "stimulus_koji" })

        // Total feedback = 5 (F1) + 5 (Koji) = 10
        assertEquals(10, state.feedbackResponses.size)
        val f1Feedback = state.getFeedbackForVideo("stimulus_f1")
        assertEquals(5, f1Feedback.size)
        assertTrue(f1Feedback.all { it.videoId == "stimulus_f1" })

        val kojiFeedback = state.getFeedbackForVideo("stimulus_koji")
        assertEquals(5, kojiFeedback.size)
        assertTrue(kojiFeedback.all { it.videoId == "stimulus_koji" })

        // Structured stimulus records
        val records = state.getStimulusRecords()
        assertEquals(2, records.size)
        assertEquals("stimulus_f1", records[0].videoId)
        assertEquals(2, records[0].performanceMetrics.size)
        assertEquals(5, records[0].feedbackResponses.size)

        assertEquals("stimulus_koji", records[1].videoId)
        assertEquals(3, records[1].performanceMetrics.size)
        assertEquals(5, records[1].feedbackResponses.size)
    }

    @Test
    fun studySession_replayingStimulusReplacesOldStimulusData() {
        val sessionViewModel = StudySessionViewModel()

        // First run of stimulus_A
        sessionViewModel.recordPerformanceMetrics(listOf(
            PerformanceMetric(videoId = "stimulus_A", cpuUsagePercent = 10.0f)
        ))
        sessionViewModel.recordFeedback(listOf(
            FeedbackResponse(videoId = "stimulus_A", questionId = "q_1", rating = 2)
        ))

        assertEquals(1, sessionViewModel.sessionState.value.performanceMetrics.size)
        assertEquals(1, sessionViewModel.sessionState.value.feedbackResponses.size)

        // Second run of stimulus_A replaces first run
        val updatedMetrics = listOf(
            PerformanceMetric(videoId = "stimulus_A", cpuUsagePercent = 25.0f),
            PerformanceMetric(videoId = "stimulus_A", cpuUsagePercent = 30.0f)
        )
        val updatedFeedback = listOf(
            FeedbackResponse(videoId = "stimulus_A", questionId = "q_1", rating = 5)
        )

        sessionViewModel.recordPerformanceMetrics(updatedMetrics)
        sessionViewModel.recordFeedback(updatedFeedback)

        assertEquals(2, sessionViewModel.sessionState.value.performanceMetrics.size)
        assertEquals(25.0f, sessionViewModel.sessionState.value.performanceMetrics[0].cpuUsagePercent)
        assertEquals(1, sessionViewModel.sessionState.value.feedbackResponses.size)
        assertEquals(5, sessionViewModel.sessionState.value.feedbackResponses[0].rating)
    }

    // =========================================================================
    // 6. End-to-End Session Data Preservation
    // =========================================================================

    @Test
    fun endToEnd_dataPreservedFromProfileThroughCompletion() {
        val sessionViewModel = StudySessionViewModel()

        // 1. Participant Setup
        val participant = Participant(
            id = "P-E2E",
            gender = "Male",
            age = 22,
            movieInterestRating = 5,
            favoriteGenre = "Sci-Fi"
        )
        sessionViewModel.saveParticipant(participant)

        // 2. Video Selection
        sessionViewModel.selectVideo("video_remote_01")

        // 3. Playback & Telemetry (Player -> Feedback transition)
        val sample1 = PerformanceMetric(videoId = "video_remote_01", playbackPositionMs = 2000L, cpuUsagePercent = 21.0f)
        val sample2 = PerformanceMetric(videoId = "video_remote_01", playbackPositionMs = 4000L, cpuUsagePercent = 23.5f)
        sessionViewModel.recordPerformanceMetrics(listOf(sample1, sample2))

        // 4. Perceptual Evaluation (Feedback -> Completion transition)
        val feedbackResponses = listOf(
            FeedbackResponse(videoId = "video_remote_01", questionId = "q_visual", rating = 5, participantId = "P-E2E"),
            FeedbackResponse(videoId = "video_remote_01", questionId = "q_immersion", rating = 4, participantId = "P-E2E"),
            FeedbackResponse(videoId = "video_remote_01", questionId = "q_synchrony", rating = 5, participantId = "P-E2E"),
            FeedbackResponse(videoId = "video_remote_01", questionId = "q_realism", rating = 4, participantId = "P-E2E"),
            FeedbackResponse(videoId = "video_remote_01", questionId = "q_satisfaction", rating = 5, participantId = "P-E2E")
        )
        sessionViewModel.recordFeedback(feedbackResponses)

        // 5. Completion State Verification
        val finalState = sessionViewModel.sessionState.value
        assertTrue("Session must be marked complete", finalState.isSessionComplete)
        assertEquals(participant, finalState.participant)
        assertEquals("video_remote_01", finalState.selectedVideoId)
        assertEquals(2, finalState.performanceMetrics.size)
        assertEquals(5, finalState.feedbackResponses.size)
        assertTrue(finalState.sessionStartTimeMs > 0L)

        // 6. Reset for Next Participant
        sessionViewModel.resetSession()
        val resetState = sessionViewModel.sessionState.value
        assertFalse(resetState.isSessionComplete)
        assertNull(resetState.participant)
        assertNull(resetState.selectedVideoId)
        assertTrue(resetState.performanceMetrics.isEmpty())
        assertTrue(resetState.feedbackResponses.isEmpty())
    }
}
