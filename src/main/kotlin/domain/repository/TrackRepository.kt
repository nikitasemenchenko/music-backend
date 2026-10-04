package ru.magnum.domain.repository

import ru.magnum.domain.model.Track

interface TrackRepository {
    suspend fun getTracks(): List<Track>
}