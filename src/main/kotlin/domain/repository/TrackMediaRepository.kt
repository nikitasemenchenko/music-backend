package ru.magnum.domain.repository

import ru.magnum.domain.model.Cover


interface TrackMediaRepository {
    suspend fun getCover(
        trackId: Int
    ): Cover?
}