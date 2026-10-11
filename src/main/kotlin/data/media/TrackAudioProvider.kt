package ru.magnum.data.media

import java.nio.file.Path
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import ru.magnum.data.database.DatabaseFactory
import ru.magnum.data.storage.FileStorage
import ru.magnum.data.table.TracksTable

class TrackAudioProvider(
    private val databaseFactory: DatabaseFactory,
    private val fileStorage: FileStorage
) {
    suspend fun getAudio(
        trackId: Int
    ): Path? {
        val audioPath = databaseFactory.dbQuery {
            TracksTable.select(
                TracksTable.audioPath
            ).where {
                TracksTable.id eq trackId
            }
                .singleOrNull()
                ?.get(TracksTable.audioPath)
        } ?: return null

        return fileStorage.getPath(audioPath)
    }
}