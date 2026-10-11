package ru.magnum.domain.usecase

import ru.magnum.domain.model.TrackCover
import ru.magnum.domain.repository.TrackMediaRepository

class GetTrackCoverUseCase(
    private val repository: TrackMediaRepository
){
    suspend operator fun invoke(
        trackId: Int
    ): TrackCover? {
        return repository.getCover(trackId)
    }
}