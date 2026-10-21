package ru.magnum.data.repository

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import ru.magnum.data.database.DatabaseFactory
import ru.magnum.data.model.TrackFilePaths
import ru.magnum.data.table.*
import ru.magnum.domain.model.CreateTrack
import ru.magnum.domain.model.UpdateTrack

class TrackAdminRepository(
    private val databaseFactory: DatabaseFactory
) {
    suspend fun createTrack(
        track: CreateTrack,
        audioPath: String,
        coverPath: String?
    ): Int {
        return databaseFactory.dbQuery {
            validate(
                track.albumIds,
                track.artistIds,
                track.genreIds
            )

            val trackId = TracksTable.insert {
                it[title] = track.title
                it[durationMs] = track.durationMs
                it[releaseDate] = track.releaseDate
                it[TracksTable.audioPath] = audioPath
                it[TracksTable.coverPath] = coverPath
            } get TracksTable.id

            track.albumIds.forEach { albumId ->
                TrackAlbumsTable.insert {
                    it[TrackAlbumsTable.trackId] = trackId
                    it[TrackAlbumsTable.albumId] = albumId
                }
            }

            track.artistIds.forEach { artistId ->
                TrackArtistsTable.insert {
                    it[TrackArtistsTable.trackId] = trackId
                    it[TrackArtistsTable.artistId] = artistId
                }
            }
            track.genreIds.forEach { genreId ->
                TrackGenresTable.insert {
                    it[TrackGenresTable.trackId] = trackId
                    it[TrackGenresTable.genreId] = genreId
                }
            }
            trackId
        }
    }


    private fun validate(
        albumIds: List<Int>,
        artistIds: List<Int>,
        genreIds: List<Int>
    ) {
        requireAllExist(
            table = AlbumsTable,
            idColumn = AlbumsTable.id,
            ids = albumIds,
            entityName = "Albums"
        )

        requireAllExist(
            table = ArtistsTable,
            idColumn = ArtistsTable.id,
            ids = artistIds,
            entityName = "Artists"
        )

        requireAllExist(
            table = GenresTable,
            idColumn = GenresTable.id,
            ids = genreIds,
            entityName = "Genres"
        )
    }

    private fun requireAllExist(
        table: Table,
        idColumn: Column<Int>,
        ids: List<Int>,
        entityName: String
    ) {
        val uniqueIds = ids.distinct()

        if (uniqueIds.isEmpty()) {
            return
        }

        val existingIds = table
            .select(idColumn)
            .where {
                idColumn inList uniqueIds
            }
            .map { row ->
                row[idColumn]
            }
            .toSet()

        val missingIds = uniqueIds.toSet() - existingIds

        require(missingIds.isEmpty()) {
            "$entityName not found: $missingIds"
        }
    }


    suspend fun deleteTrack(
        id: Int
    ): TrackFilePaths? {
        return databaseFactory.dbQuery {
            val track = TracksTable.selectAll().where { TracksTable.id eq id }.singleOrNull()
            if (track == null) {
                return@dbQuery null
            }
            val deletedTrack = TrackFilePaths(
                audioPath = track[TracksTable.audioPath],
                coverPath = track[TracksTable.coverPath]
            )
            TracksTable.deleteWhere { TracksTable.id eq id }
            deletedTrack
        }
    }

    suspend fun updateTrack(
        id: Int,
        track: UpdateTrack,
        newAudioPath: String?,
        newCoverPath: String?
    ): TrackFilePaths? {
        return databaseFactory.dbQuery {
            val existingTrack = TracksTable
                .selectAll()
                .where {
                    TracksTable.id eq id
                }
                .singleOrNull()
                ?: return@dbQuery null

            validate(
                track.albumIds,
                track.artistIds,
                track.genreIds
            )
            val oldFilePaths = TrackFilePaths(
                audioPath = existingTrack[TracksTable.audioPath],
                coverPath = existingTrack[TracksTable.coverPath]
            )

            TracksTable.update(
                where = { TracksTable.id eq id }
            ) {
                it[title] = track.title
                it[durationMs] = track.durationMs
                it[releaseDate] = track.releaseDate
                if (newAudioPath != null) {
                    it[audioPath] = newAudioPath
                }
                if (newCoverPath != null) {
                    it[coverPath] = newCoverPath
                }
            }

            TrackAlbumsTable.deleteWhere {
                TrackAlbumsTable.trackId eq id
            }
            track.albumIds.forEach { albumId ->
                TrackAlbumsTable.insert {
                    it[TrackAlbumsTable.trackId] = id
                    it[TrackAlbumsTable.albumId] = albumId
                }
            }

            TrackArtistsTable.deleteWhere {
                TrackArtistsTable.trackId eq id
            }
            track.artistIds.forEach { artistId ->
                TrackArtistsTable.insert {
                    it[TrackArtistsTable.trackId] = id
                    it[TrackArtistsTable.artistId] = artistId
                }
            }

            TrackGenresTable.deleteWhere {
                TrackGenresTable.trackId eq id
            }
            track.genreIds.forEach { genreId ->
                TrackGenresTable.insert {
                    it[TrackGenresTable.trackId] = id
                    it[TrackGenresTable.genreId] = genreId
                }
            }
            oldFilePaths
        }
    }
}