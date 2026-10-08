package com.haptix.app.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import com.haptix.app.data.model.FeedbackResponse
import com.haptix.app.data.model.Participant
import com.haptix.app.data.model.PerformanceMetric
import com.haptix.app.data.model.VideoItem
import com.haptix.app.ui.theme.CyberThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Structured record grouping hardware performance samples and perceptual feedback
 * for a single stimulus within the broader experimental session.
 */
data class StimulusSessionRecord(
    val videoId: String,
    val performanceMetrics: List<PerformanceMetric> = emptyList(),
    val feedbackResponses: List<FeedbackResponse> = emptyList()
)

/**
 * Encapsulates the runtime state of an active experimental session,
 * including persistent theme mode, participant demographics, questionnaire ratings,
 * and hardware performance telemetry across one or more stimuli.
 */
data class StudySessionState(
    val participant: Participant? = null,
    val selectedVideoId: String? = null,
    val isHapticsEnabled: Boolean = true,
    val isHapticSupported: Boolean = true,
    val feedbackResponses: List<FeedbackResponse> = emptyList(),
    val performanceMetrics: List<PerformanceMetric> = emptyList(),
    val isSessionComplete: Boolean = false,
    val themeMode: CyberThemeMode = CyberThemeMode.DARK,
    val sessionStartTimeMs: Long = System.currentTimeMillis()
) {
    /**
     * Unique stimulus IDs that have been evaluated in this session.
     */
    val evaluatedVideoIds: List<String>
        get() = (feedbackResponses.map { it.videoId } + performanceMetrics.map { it.videoId }).distinct()

    /**
     * Returns all performance samples associated with [videoId].
     */
    fun getMetricsForVideo(videoId: String): List<PerformanceMetric> =
        performanceMetrics.filter { it.videoId == videoId }

    /**
     * Returns all feedback responses associated with [videoId].
     */
    fun getFeedbackForVideo(videoId: String): List<FeedbackResponse> =
        feedbackResponses.filter { it.videoId == videoId }

    /**
     * Returns segregated records per stimulus, facilitating subsequent backend serialization.
     */
    fun getStimulusRecords(): List<StimulusSessionRecord> =
        evaluatedVideoIds.map { id ->
            StimulusSessionRecord(
                videoId = id,
                performanceMetrics = getMetricsForVideo(id),
                feedbackResponses = getFeedbackForVideo(id)
            )
        }
}

/**
 * Session-level ViewModel managing participant demographics, stimulus selection,
 * persistent haptics settings, theme transitions, and questionnaire submissions across the study lifecycle.
 */
class StudySessionViewModel(
    initialHapticSupported: Boolean = true,
    initialThemeMode: CyberThemeMode = CyberThemeMode.DARK
) : ViewModel() {

    private val _sessionState = MutableStateFlow(
        StudySessionState(
            isHapticSupported = initialHapticSupported,
            isHapticsEnabled = initialHapticSupported,
            themeMode = initialThemeMode
        )
    )
    val sessionState: StateFlow<StudySessionState> = _sessionState.asStateFlow()

    fun updateHapticHardwareSupport(supported: Boolean) {
        _sessionState.value = _sessionState.value.copy(
            isHapticSupported = supported,
            isHapticsEnabled = if (!supported) false else _sessionState.value.isHapticsEnabled
        )
    }

    fun setHapticsEnabled(enabled: Boolean) {
        if (!_sessionState.value.isHapticSupported) return
        _sessionState.value = _sessionState.value.copy(isHapticsEnabled = enabled)
    }

    fun setThemeMode(mode: CyberThemeMode) {
        _sessionState.value = _sessionState.value.copy(themeMode = mode)
    }

    fun toggleThemeMode() {
        val next = if (_sessionState.value.themeMode == CyberThemeMode.DARK) {
            CyberThemeMode.LIGHT
        } else {
            CyberThemeMode.DARK
        }
        _sessionState.value = _sessionState.value.copy(themeMode = next)
    }

    fun saveParticipant(participant: Participant) {
        _sessionState.value = _sessionState.value.copy(participant = participant)
    }

    fun selectVideo(videoId: String) {
        _sessionState.value = _sessionState.value.copy(selectedVideoId = videoId)
    }

    fun recordFeedback(responses: List<FeedbackResponse>) {
        if (responses.isEmpty()) return
        val newVideoIds = responses.map { it.videoId }.toSet()
        val filtered = _sessionState.value.feedbackResponses.filterNot { it.videoId in newVideoIds }
        _sessionState.value = _sessionState.value.copy(
            feedbackResponses = filtered + responses,
            isSessionComplete = true
        )
    }

    fun recordPerformanceMetrics(metrics: List<PerformanceMetric>) {
        if (metrics.isEmpty()) return
        val newVideoIds = metrics.map { it.videoId }.toSet()
        val filtered = _sessionState.value.performanceMetrics.filterNot { it.videoId in newVideoIds }
        _sessionState.value = _sessionState.value.copy(
            performanceMetrics = filtered + metrics
        )
    }

    fun clearStimulusData(videoId: String) {
        _sessionState.value = _sessionState.value.copy(
            feedbackResponses = _sessionState.value.feedbackResponses.filterNot { it.videoId == videoId },
            performanceMetrics = _sessionState.value.performanceMetrics.filterNot { it.videoId == videoId }
        )
    }

    fun resetSession() {
        val currentTheme = _sessionState.value.themeMode
        val hardwareSupported = _sessionState.value.isHapticSupported
        _sessionState.value = StudySessionState(
            isHapticSupported = hardwareSupported,
            isHapticsEnabled = hardwareSupported,
            themeMode = currentTheme
        )
    }
}
