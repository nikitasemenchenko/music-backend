package ru.magnum.application

import java.nio.file.Path
import ru.magnum.data.repository.TrackAdminRepository
import ru.magnum.data.storage.FileStorage
import ru.magnum.domain.model.CreateTrack

class CreateTrackService(
    private val repository: TrackAdminRepository,
    private val fileStorage: FileStorage
) {
    suspend fun createTrack(
        track: CreateTrack,
        audioTempFile: Path,
        audioExtension: String,
        coverTempFile: Path?,
        coverExtension: String?
    ): Int {
        require((coverExtension == null) == (coverTempFile == null))
        var audioPath: String? = null
        var coverPath: String? = null

        try {
            audioPath = fileStorage.moveTempFile(
                tempFile = audioTempFile,
                directory = "audio",
                extension = audioExtension
            )
            if (coverTempFile != null && coverExtension != null) {
                coverPath = fileStorage.moveTempFile(
                    tempFile = coverTempFile,
                    directory = "covers",
                    extension = coverExtension
                )
            }
            return repository.createTrack(
                track = track,
                audioPath = audioPath,
                coverPath = coverPath
            )
        } catch (e: Exception) {
            audioPath?.let {
                fileStorage.delete(it)
            }
            coverPath?.let {
                fileStorage.delete(it)
            }
            throw e
        } finally {
            fileStorage.deleteTemp(
                audioTempFile
            )
            coverTempFile?.let {
                fileStorage.deleteTemp(it)
            }
        }
    }
}