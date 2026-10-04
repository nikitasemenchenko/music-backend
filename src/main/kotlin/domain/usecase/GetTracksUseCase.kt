package ru.magnum.domain.usecase

import ru.magnum.domain.model.Track
import ru.magnum.domain.repository.TrackRepository

class GetTracksUseCase(
    private val repository: TrackRepository
) {
    suspend operator fun invoke(): List<Track> {
        return repository.getTracks()
    }
}