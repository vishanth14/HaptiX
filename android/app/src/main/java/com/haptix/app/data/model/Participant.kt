package com.haptix.app.data.model

/**
 * Represents experimental study participant details.
 *
 * Captures demographic and movie preference information for research correlation.
 *
 * @param id Unique participant identifier.
 * @param gender Gender of the participant.
 * @param age Age in years.
 * @param movieInterestRating Self-reported interest in movies on a 1-5 Likert scale.
 * @param favoriteGenre Participant's favorite film/video genre.
 * @param sessionNumber Experimental session sequence index.
 * @param notes Optional researcher commentary or notes.
 */
data class Participant(
    val id: String = "",
    val gender: String = "",
    val age: Int = 0,
    val movieInterestRating: Int = 3,
    val favoriteGenre: String = "",
    val sessionNumber: Int = 1,
    val notes: String = ""
) {
    init {
        require(movieInterestRating in 1..5) {
            "Movie interest rating must be between 1 and 5, was $movieInterestRating"
        }
        require(age >= 0) {
            "Age cannot be negative, was $age"
        }
    }
}
