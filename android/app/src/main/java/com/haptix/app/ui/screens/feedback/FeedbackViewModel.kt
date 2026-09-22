package com.haptix.app.ui.screens.feedback

import androidx.lifecycle.ViewModel
import com.haptix.app.data.model.FeedbackResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI State representation for the Feedback screen capturing 5 rating values (1–5 scale).
 */
data class FeedbackUiState(
    val videoId: String = "",
    val rating1: Int = 3,
    val rating2: Int = 3,
    val rating3: Int = 3,
    val rating4: Int = 3,
    val rating5: Int = 3,
    val comments: String = "",
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false
)

/**
 * Metadata definition for the 5 fixed research evaluation questions.
 */
data class EvaluationQuestion(
    val id: String,
    val number: Int,
    val text: String,
    val lowAnchor: String = "1 - Poor",
    val highAnchor: String = "5 - Excellent"
)

val STUDY_QUESTIONS = listOf(
    EvaluationQuestion(
        id = "q_visual",
        number = 1,
        text = "How clear and visually engaging was the video clip?",
        lowAnchor = "1 - Low Clarity",
        highAnchor = "5 - High Clarity"
    ),
    EvaluationQuestion(
        id = "q_immersion",
        number = 2,
        text = "How immersive was the overall sensory experience?",
        lowAnchor = "1 - Not Immersive",
        highAnchor = "5 - Highly Immersive"
    ),
    EvaluationQuestion(
        id = "q_synchrony",
        number = 3,
        text = "How well did the tactile sensations synchronize with video events?",
        lowAnchor = "1 - Poorly Aligned",
        highAnchor = "5 - Perfectly Aligned"
    ),
    EvaluationQuestion(
        id = "q_realism",
        number = 4,
        text = "How natural and realistic did the haptic sensations feel?",
        lowAnchor = "1 - Artificial",
        highAnchor = "5 - Very Natural"
    ),
    EvaluationQuestion(
        id = "q_satisfaction",
        number = 5,
        text = "What is your overall satisfaction with this experimental stimulus?",
        lowAnchor = "1 - Unsatisfied",
        highAnchor = "5 - Very Satisfied"
    )
)

/**
 * ViewModel managing 1–5 participant rating responses and submission state.
 */
class FeedbackViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FeedbackUiState())
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun setVideoId(videoId: String) {
        _uiState.value = _uiState.value.copy(videoId = videoId)
    }

    fun updateRating(index: Int, rating: Int) {
        val clamped = rating.coerceIn(1, 5)
        _uiState.value = when (index) {
            1 -> _uiState.value.copy(rating1 = clamped)
            2 -> _uiState.value.copy(rating2 = clamped)
            3 -> _uiState.value.copy(rating3 = clamped)
            4 -> _uiState.value.copy(rating4 = clamped)
            5 -> _uiState.value.copy(rating5 = clamped)
            else -> _uiState.value
        }
    }

    fun getRatingForQuestion(index: Int): Int {
        return when (index) {
            1 -> _uiState.value.rating1
            2 -> _uiState.value.rating2
            3 -> _uiState.value.rating3
            4 -> _uiState.value.rating4
            5 -> _uiState.value.rating5
            else -> 3
        }
    }

    fun updateComments(comments: String) {
        _uiState.value = _uiState.value.copy(comments = comments)
    }

    fun buildResponses(participantId: String, videoId: String): List<FeedbackResponse> {
        val state = _uiState.value
        return listOf(
            FeedbackResponse(videoId = videoId, questionId = "q_visual", rating = state.rating1, participantId = participantId),
            FeedbackResponse(videoId = videoId, questionId = "q_immersion", rating = state.rating2, participantId = participantId),
            FeedbackResponse(videoId = videoId, questionId = "q_synchrony", rating = state.rating3, participantId = participantId),
            FeedbackResponse(videoId = videoId, questionId = "q_realism", rating = state.rating4, participantId = participantId),
            FeedbackResponse(videoId = videoId, questionId = "q_satisfaction", rating = state.rating5, participantId = participantId)
        )
    }

    fun submitFeedback(
        participantId: String,
        hapticEnabled: Boolean,
        onSubmitted: (List<FeedbackResponse>) -> Unit = {}
    ) {
        val videoId = _uiState.value.videoId
        val responses = buildResponses(participantId, videoId)
        _uiState.value = _uiState.value.copy(isSubmitted = true)
        onSubmitted(responses)
    }
}
