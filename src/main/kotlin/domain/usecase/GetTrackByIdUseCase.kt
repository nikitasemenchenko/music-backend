package ru.magnum.domain.usecase

import ru.magnum.domain.model.Track
import ru.magnum.domain.repository.TrackRepository

class GetTrackByIdUseCase(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(id: Int): Track? {
        return repository.getTrackById(id)
    }
}