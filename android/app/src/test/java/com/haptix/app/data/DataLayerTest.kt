package com.haptix.app.data

import com.haptix.app.data.model.FeedbackResponse
import com.haptix.app.data.model.HapticEvent
import com.haptix.app.data.model.HapticEventType
import com.haptix.app.data.model.HapticPattern
import com.haptix.app.data.model.Participant
import com.haptix.app.data.model.PerformanceMetric
import com.haptix.app.data.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataLayerTest {

    // 1. Participant model test
    @Test
    fun participantModel_capturesDemographicsAndPreferences() {
        val participant = Participant(
            id = "p_101",
            gender = "Non-binary",
            age = 24,
            movieInterestRating = 5,
            favoriteGenre = "Sci-Fi",
            sessionNumber = 2,
            notes = "Experienced with VR and tactile devices"
        )

        assertEquals("p_101", participant.id)
        assertEquals("Non-binary", participant.gender)
        assertEquals(24, participant.age)
        assertEquals(5, participant.movieInterestRating)
        assertEquals("Sci-Fi", participant.favoriteGenre)
        assertEquals(2, participant.sessionNumber)
        assertEquals("Experienced with VR and tactile devices", participant.notes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun participantModel_rejectsInvalidMovieInterestRating() {
        Participant(
            gender = "Female",
            age = 30,
            movieInterestRating = 6, // Exceeds 1-5 scale
            favoriteGenre = "Drama"
        )
    }

    // 2. VideoItem model test
    @Test
    fun videoItemModel_capturesMetadataAndResourceInfo() {
        val video = VideoItem(
            id = "vid_01",
            title = "Cinematic Action Sequence",
            durationMs = 45000L,
            thumbnailResUri = "res/drawable/thumb_01.png",
            videoResUri = "content://media/external/video/01",
            description = "High dynamic action scenes",
            hapticConfigResName = "haptics_vid_01"
        )

        assertEquals("vid_01", video.id)
        assertEquals("Cinematic Action Sequence", video.title)
        assertEquals(45000L, video.durationMs)
        assertEquals("res/drawable/thumb_01.png", video.thumbnailResUri)
        assertEquals("content://media/external/video/01", video.videoResUri)
        assertEquals("High dynamic action scenes", video.description)
        assertEquals("haptics_vid_01", video.hapticConfigResName)
    }

    // 3. HapticEvent model test
    @Test
    fun hapticEventModel_platformIndependentRepresentation() {
        val event = HapticEvent(
            type = HapticEventType.CONTINUOUS,
            startTimeMs = 1200L,
            durationMs = 800L,
            intensity = 0.85f,
            sharpness = 0.4f,
            parameters = mapOf("frequencyHz" to 150, "envelope" to "smooth")
        )

        assertEquals(HapticEventType.CONTINUOUS, event.type)
        assertEquals(1200L, event.startTimeMs)
        assertEquals(1200L, event.timestampMs) // Alias compatibility
        assertEquals(800L, event.durationMs)
        assertEquals(2000L, event.endTimeMs)
        assertEquals(0.85f, event.intensity, 0.001f)
        assertEquals(0.4f, event.sharpness, 0.001f)
        assertEquals(150, event.parameters["frequencyHz"])
        assertEquals("smooth", event.parameters["envelope"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun hapticEventModel_rejectsOutOfRangeIntensity() {
        HapticEvent(
            startTimeMs = 100L,
            durationMs = 50L,
            intensity = 1.5f // Exceeds 1.0f max
        )
    }

    // 4. HapticPattern model test
    @Test
    fun hapticPatternModel_containsMultipleEvents() {
        val event1 = HapticEvent(type = HapticEventType.TRANSIENT, startTimeMs = 500L, durationMs = 50L, intensity = 1.0f)
        val event2 = HapticEvent(type = HapticEventType.CONTINUOUS, startTimeMs = 1200L, durationMs = 600L, intensity = 0.7f)

        val pattern = HapticPattern(
            videoId = "vid_01",
            version = "1.0",
            events = listOf(event1, event2)
        )

        assertEquals("vid_01", pattern.videoId)
        assertEquals("1.0", pattern.version)
        assertEquals(2, pattern.eventCount)
        assertEquals(2, pattern.events.size)
        assertEquals(1800L, pattern.totalDurationMs) // 1200 + 600 = 1800
    }

    // 5. FeedbackResponse model test
    @Test
    fun feedbackResponseModel_capturesVideoIdQuestionIdAndRating() {
        val feedback = FeedbackResponse(
            videoId = "vid_01",
            questionId = "q_tactile_immersion",
            rating = 4,
            participantId = "p_101"
        )

        assertEquals("vid_01", feedback.videoId)
        assertEquals("q_tactile_immersion", feedback.questionId)
        assertEquals(4, feedback.rating)
        assertEquals("p_101", feedback.participantId)
        assertTrue(feedback.timestamp > 0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun feedbackResponseModel_rejectsRatingOutOfRange() {
        FeedbackResponse(
            videoId = "vid_01",
            questionId = "q_immersion",
            rating = 0 // Below 1-5 scale
        )
    }

    // 6. PerformanceMetric model test
    @Test
    fun performanceMetricModel_capturesSystemAndThermalMetrics() {
        val metric = PerformanceMetric(
            timestamp = 1690000000000L,
            cpuUsagePercent = 28.5f,
            gpuUsagePercent = 42.0f,
            deviceTemperatureCelsius = 36.8f,
            videoId = "vid_01",
            fps = 59.8f,
            memoryUsageMb = 145L
        )

        assertEquals(1690000000000L, metric.timestamp)
        assertEquals(1690000000000L, metric.timestampMs)
        assertNotNull(metric.cpuUsagePercent)
        assertEquals(28.5f, metric.cpuUsagePercent!!, 0.01f)
        assertNotNull(metric.gpuUsagePercent)
        assertEquals(42.0f, metric.gpuUsagePercent!!, 0.01f)
        assertNotNull(metric.deviceTemperatureCelsius)
        assertEquals(36.8f, metric.deviceTemperatureCelsius!!, 0.01f)
        assertEquals("vid_01", metric.videoId)
    }

    @Test
    fun performanceMetricModel_handlesGpuUnavailableGracefully() {
        val metric = PerformanceMetric(
            cpuUsagePercent = 15.0f,
            gpuUsagePercent = null, // GPU unavailable on older devices
            deviceTemperatureCelsius = 34.0f
        )

        assertNull(metric.gpuUsagePercent)
        assertNotNull(metric.cpuUsagePercent)
        assertEquals(15.0f, metric.cpuUsagePercent!!, 0.01f)
        assertNotNull(metric.deviceTemperatureCelsius)
        assertEquals(34.0f, metric.deviceTemperatureCelsius!!, 0.01f)
    }
}
