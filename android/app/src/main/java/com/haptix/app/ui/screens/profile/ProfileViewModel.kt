package com.haptix.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haptix.app.data.model.Participant
import com.haptix.app.data.repository.ParticipantRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State representation for Participant Setup.
 */
data class ProfileUiState(
    val participantId: String = "",
    val participantName: String = "",
    val sessionNumber: Int = 1,
    val gender: String = "",
    val ageText: String = "",
    val movieInterestRating: Int = 3,
    val favoriteGenre: String = "",
    val ageError: String? = null,
    val isReady: Boolean = false
) {
    /**
     * True if all required fields meet study validation criteria.
     */
    val isValid: Boolean
        get() = gender.isNotBlank() &&
                favoriteGenre.isNotBlank() &&
                ageError == null &&
                ageText.isNotBlank() &&
                (ageText.toIntOrNull() ?: -1) in 18..100
}

/**
 * ViewModel managing Participant Setup screen state, demographic validation, and repository persistence.
 */
class ProfileViewModel(
    private val participantRepository: ParticipantRepository? = null,
    private val coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope get() = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            participantId = "P-${(System.currentTimeMillis() % 9000 + 1000)}"
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun updateParticipantId(id: String) {
        _uiState.value = _uiState.value.copy(participantId = id)
    }

    fun updateParticipantName(name: String) {
        _uiState.value = _uiState.value.copy(participantName = name)
    }

    fun updateGender(gender: String) {
        _uiState.value = _uiState.value.copy(gender = gender)
    }

    fun updateAge(ageText: String) {
        val filtered = ageText.filter { it.isDigit() }
        val error = when {
            filtered.isEmpty() -> "Age is required"
            filtered.toIntOrNull() == null -> "Please enter a valid number"
            filtered.toInt() < 18 -> "Participant must be at least 18 years old"
            filtered.toInt() > 100 -> "Please enter a valid age (up to 100)"
            else -> null
        }
        _uiState.value = _uiState.value.copy(
            ageText = filtered,
            ageError = error
        )
    }

    fun updateMovieInterest(rating: Int) {
        _uiState.value = _uiState.value.copy(
            movieInterestRating = rating.coerceIn(1, 5)
        )
    }

    fun updateFavoriteGenre(genre: String) {
        _uiState.value = _uiState.value.copy(favoriteGenre = genre)
    }

    /**
     * Builds and saves a validated [Participant] record into the repository.
     */
    fun saveParticipant(onSuccess: (Participant) -> Unit) {
        val state = _uiState.value
        if (!state.isValid) return

        val age = state.ageText.toIntOrNull() ?: return
        val participant = Participant(
            id = state.participantId.ifBlank { "P-Default" },
            gender = state.gender,
            age = age,
            movieInterestRating = state.movieInterestRating,
            favoriteGenre = state.favoriteGenre,
            sessionNumber = state.sessionNumber
        )

        scope.launch {
            participantRepository?.saveParticipant(participant)
            onSuccess(participant)
        }
    }
}
