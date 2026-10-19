package ru.magnum.data.repository

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import ru.magnum.data.database.DatabaseFactory
import ru.magnum.data.storage.FileStorage
import ru.magnum.data.table.TracksTable
import ru.magnum.domain.model.Cover
import ru.magnum.domain.repository.TrackMediaRepository

class TrackMediaRepositoryImpl(
    private val databaseFactory: DatabaseFactory,
    private val fileStorage: FileStorage
) : TrackMediaRepository {
    override suspend fun getCover(trackId: Int): Cover? {
        val coverPath = databaseFactory.dbQuery {
            TracksTable.select(
                TracksTable.coverPath
            )
            .where {
                TracksTable.id eq trackId
            }
                .singleOrNull()
                ?.get(TracksTable.coverPath)
        }
        ?: return null

        val bytes = fileStorage.read(coverPath) ?: return null

        return Cover(
            bytes = bytes,
            contentType = resolveContentType(coverPath)
        )
    }

    private fun resolveContentType(
        path: String
    ): String {
        return when (
            path.substringAfterLast(
                delimiter = ".",
                missingDelimiterValue = ""
            ).lowercase()
        ) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }
}