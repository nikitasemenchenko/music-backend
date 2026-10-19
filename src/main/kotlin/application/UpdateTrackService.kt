package ru.magnum.application

import ru.magnum.data.repository.TrackAdminRepository
import ru.magnum.data.storage.FileStorage
import ru.magnum.domain.model.UpdateTrack
import java.nio.file.Path

class UpdateTrackService(
    private val repository: TrackAdminRepository,
    private val fileStorage: FileStorage
) {
    suspend fun updateTrack(
        id: Int,
        track: UpdateTrack,
        audioTempFile: Path?,
        audioExtension: String?,
        coverTempFile: Path?,
        coverExtension: String?
    ): Boolean {
        require((audioTempFile == null) == (audioExtension == null))
        require((coverTempFile == null) == (coverExtension == null))

        var newAudioPath: String? = null
        var newCoverPath: String? = null

        try {
            if(audioTempFile !=  null && audioExtension != null) {
                newAudioPath = fileStorage.moveTempFile(
                    tempFile = audioTempFile,
                    directory = "audio",
                    extension = audioExtension
                )
            }
            if(coverTempFile !=  null && coverExtension != null) {
                newCoverPath = fileStorage.moveTempFile(
                    tempFile = coverTempFile,
                    directory = "covers",
                    extension = coverExtension
                )
            }
            val oldFilePaths = repository.updateTrack(
                id = id,
                track = track,
                newAudioPath = newAudioPath,
                newCoverPath = newCoverPath
            )
            if(oldFilePaths == null){
                newAudioPath?.let {
                    fileStorage.delete(it)
                }
                newCoverPath?.let {
                    fileStorage.delete(it)
                }
                return false
            }

            if(newAudioPath != null && oldFilePaths.audioPath != newAudioPath) {
                deleteSafely(oldFilePaths.audioPath)
            }

            if(newCoverPath != null && oldFilePaths.coverPath != null && oldFilePaths.coverPath != newCoverPath){
                deleteSafely(oldFilePaths.coverPath)
            }
            return true

        } catch (e: Exception) {
            newAudioPath?.let {
                fileStorage.delete(it)
            }
            newCoverPath?.let {
                fileStorage.delete(it)
            }
            throw e
        } finally {
            audioTempFile?.let {
                fileStorage.deleteTemp(it)
            }
            coverTempFile?.let {
                fileStorage.deleteTemp(it)
            }
        }
    }

    private suspend fun deleteSafely(
        path: String
    ) {
        try {
                fileStorage.delete(path)
        } catch (e: Exception) {


        }
    }
}