package ru.magnum.data.repository

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import ru.magnum.data.database.DatabaseFactory
import ru.magnum.data.table.AlbumsTable
import ru.magnum.data.table.ArtistsTable
import ru.magnum.data.table.GenresTable
import ru.magnum.data.table.TrackArtistsTable
import ru.magnum.data.table.TrackGenresTable
import ru.magnum.data.table.TracksTable
import ru.magnum.domain.model.CreateTrack

class TrackAdminRepository(
    private val databaseFactory: DatabaseFactory
) {
    suspend fun createTrack(
        track: CreateTrack,
        audioPath: String,
        coverPath: String?
    ): Int {
        return databaseFactory.dbQuery {
            validate(track)

            val trackId = TracksTable.insert {
                it[title] = track.title
                it[albumId] = track.albumId
                it[durationMs] = track.durationMs
                it[releaseYear] = track.releaseYear
                it[TracksTable.audioPath] = audioPath
                it[TracksTable.coverPath] = coverPath
            } get TracksTable.id

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
        track: CreateTrack
    ) {
        if(track.albumId != null) {
            val albumExists = AlbumsTable.selectAll().where { AlbumsTable.id eq track.albumId }.any()
            require(albumExists) { "Album ${track.albumId} not found" }

        }
        track.artistIds.forEach { artistId ->
            val exists = ArtistsTable.selectAll().where { ArtistsTable.id eq artistId }.any()
            require(exists) { "Artist $artistId not found" }
        }
        track.genreIds.forEach { genreId ->
            val exists = GenresTable.selectAll().where { GenresTable.id eq genreId }.any()
            require(exists) { "Genre $genreId not found" }
        }
    }
}