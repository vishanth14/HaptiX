package com.haptix.app.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import com.haptix.app.data.model.FeedbackResponse
import com.haptix.app.data.model.Participant
import com.haptix.app.data.model.VideoItem
import com.haptix.app.ui.theme.CyberThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Encapsulates the runtime state of an active experimental session,
 * including persistent theme mode, participant demographics, and questionnaire ratings.
 */
data class StudySessionState(
    val participant: Participant? = null,
    val selectedVideoId: String? = null,
    val isHapticsEnabled: Boolean = true,
    val isHapticSupported: Boolean = true,
    val feedbackResponses: List<FeedbackResponse> = emptyList(),
    val isSessionComplete: Boolean = false,
    val themeMode: CyberThemeMode = CyberThemeMode.DARK
)

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
        _sessionState.value = _sessionState.value.copy(
            feedbackResponses = responses,
            isSessionComplete = true
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
