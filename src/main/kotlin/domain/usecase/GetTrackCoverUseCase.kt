package ru.magnum.domain.usecase


import ru.magnum.domain.model.Cover
import ru.magnum.domain.repository.TrackMediaRepository

class GetTrackCoverUseCase(
    private val repository: TrackMediaRepository
){
    suspend operator fun invoke(
        trackId: Int
    ): Cover? {
        return repository.getCover(trackId)
    }
}