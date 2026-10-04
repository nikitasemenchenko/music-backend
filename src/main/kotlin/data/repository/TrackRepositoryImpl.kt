package ru.magnum.data.repository

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import ru.magnum.data.database.DatabaseFactory
import ru.magnum.data.table.AlbumsTable
import ru.magnum.data.table.ArtistsTable
import ru.magnum.data.table.GenresTable
import ru.magnum.data.table.TrackArtistsTable
import ru.magnum.data.table.TrackGenresTable
import ru.magnum.data.table.TracksTable
import ru.magnum.domain.model.Album
import ru.magnum.domain.model.Artist
import ru.magnum.domain.model.Genre
import ru.magnum.domain.model.Track
import ru.magnum.domain.repository.TrackRepository

class TrackRepositoryImpl(
    private val databaseFactory: DatabaseFactory
) : TrackRepository {
    override suspend fun getTracks(): List<Track> {
        return databaseFactory.dbQuery {
            val trackRows = TracksTable.join(
                otherTable = AlbumsTable,
                joinType = JoinType.LEFT,
                additionalConstraint = {
                    TracksTable.albumId eq AlbumsTable.id
                }
            )
                .selectAll()
                .toList()

            if(trackRows.isEmpty()){
                return@dbQuery emptyList()
            }

            val trackIds = trackRows.map {
                it[TracksTable.id]
            }

            val artistsByTrackId = loadArtists(trackIds)
            val genresByTrackIds = loadGenres(trackIds)

            trackRows.map { row ->
                row.toTrack(
                    artists = artistsByTrackId[row[TracksTable.id]].orEmpty(),
                    genres = genresByTrackIds[row[TracksTable.id]].orEmpty()
                )
            }

        }
    }

    private fun loadArtists(trackIds: List<Int>): Map<Int, List<Artist>> {
        return TrackArtistsTable.join(
            otherTable = ArtistsTable,
            joinType = JoinType.INNER,
            additionalConstraint = {
                TrackArtistsTable.artistId eq ArtistsTable.id
            }
        )
            .select(
                TrackArtistsTable.trackId,
                ArtistsTable.id,
                ArtistsTable.name
            )
            .where {
                TrackArtistsTable.trackId inList trackIds
            }
            .map { row ->
                row[TrackArtistsTable.trackId] to Artist(
                    id = row[ArtistsTable.id],
                    name = row[ArtistsTable.name]
                )
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second }
            )
    }

    private fun loadGenres(trackIds: List<Int>): Map<Int, List<Genre>> {
        return TrackGenresTable.join(
            otherTable = GenresTable,
            joinType = JoinType.INNER,
            additionalConstraint = {
                TrackGenresTable.genreId eq GenresTable.id
            }
        )
            .select(
                TrackGenresTable.trackId,
                GenresTable.id,
                GenresTable.name
            )
            .where {
                TrackGenresTable.trackId inList trackIds
            }
            .map { row ->
                row[TrackGenresTable.trackId] to Genre(
                    id = row[GenresTable.id],
                    name = row[GenresTable.name]
                )
            }
            .groupBy(
                keySelector = {it.first},
                valueTransform = {it.second}
            )
    }

    private fun ResultRow.toTrack(
        artists: List<Artist>,
        genres: List<Genre>
    ): Track {
        val albumId = this[TracksTable.albumId]
        val album = if (albumId != null) {
            Album(
                id = albumId,
                title = this[AlbumsTable.title],
            )
        } else {
            null
        }
        return Track(
            id = this[TracksTable.id],
            title = this[TracksTable.title],
            artists = artists,
            album = album,
            genres = genres,
            durationMs = this[TracksTable.durationMs],
            releaseYear = this[TracksTable.releaseYear],
            createdAt = this[TracksTable.createdAt],
        )
    }
}