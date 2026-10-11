package ru.magnum.domain.repository

import ru.magnum.domain.model.TrackCover

interface TrackMediaRepository {
    suspend fun getCover(
        trackId: Int
    ): TrackCover?
}