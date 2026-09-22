package com.haptix.app.data.repository

import com.haptix.app.data.model.Participant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository interface for managing experimental participant session data.
 */
interface ParticipantRepository {
    val currentParticipant: Flow<Participant?>
    suspend fun saveParticipant(participant: Participant)
    suspend fun getParticipant(): Participant?
    suspend fun clearParticipant()
}

/**
 * In-memory placeholder implementation of [ParticipantRepository].
 */
class InMemoryParticipantRepository : ParticipantRepository {
    private val _participantState = MutableStateFlow<Participant?>(null)
    override val currentParticipant: Flow<Participant?> = _participantState.asStateFlow()

    override suspend fun saveParticipant(participant: Participant) {
        _participantState.value = participant
    }

    override suspend fun getParticipant(): Participant? {
        return _participantState.value
    }

    override suspend fun clearParticipant() {
        _participantState.value = null
    }
}
