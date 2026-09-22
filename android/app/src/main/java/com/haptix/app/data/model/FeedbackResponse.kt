package com.haptix.app.data.model

/**
 * Represents participant feedback rating collected after an experimental video playback session.
 *
 * @param videoId Associated video stimulus identifier.
 * @param questionId Questionnaire item identifier (e.g. "q_immersion", "q_synchrony").
 * @param rating User assessment rating on a 1–5 Likert scale.
 * @param participantId Identifier of the participating research subject.
 * @param timestamp Epoch timestamp when the rating was submitted.
 */
data class FeedbackResponse(
    val videoId: String,
    val questionId: String,
    val rating: Int,
    val participantId: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(rating in 1..5) {
            "Feedback rating must be on a 1–5 scale, was $rating"
        }
    }
}
