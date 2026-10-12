package ru.magnum.application

import ru.magnum.data.repository.TrackAdminRepository
import ru.magnum.data.storage.FileStorage

class DeleteTrackService(
    private val repository: TrackAdminRepository,
    private val fileStorage: FileStorage
){
    suspend fun deleteTrack(
        id: Int
    ): Boolean {
        val track = repository.deleteTrack(id) ?: return false

        fileStorage.delete(
            track.audioPath
        )
        track.coverPath?. let { fileStorage.delete(it) }
        return true
    }
}