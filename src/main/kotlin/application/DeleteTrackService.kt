package ru.magnum.application

import org.slf4j.LoggerFactory
import ru.magnum.data.repository.TrackAdminRepository
import ru.magnum.data.storage.FileStorage

class DeleteTrackService(
    private val repository: TrackAdminRepository,
    private val fileStorage: FileStorage
) {
    private val logger = LoggerFactory.getLogger(DeleteTrackService::class.java)

    suspend fun deleteTrack(id: Int): Boolean {
        val track = repository.deleteTrack(id) ?: return false

        deleteSafely(track.audioPath)

        track.coverPath?.let {
            deleteSafely(it)
        }

        return true
    }

    private suspend fun deleteSafely(path: String) {
        try {
            fileStorage.delete(path)
        } catch (e: Exception) {
            logger.error(
                "Failed to delete media file after track deletion: {}",
                path,
                e
            )
        }
    }
}