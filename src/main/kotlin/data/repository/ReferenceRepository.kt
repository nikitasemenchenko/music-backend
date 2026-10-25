package ru.magnum.data.repository

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import ru.magnum.data.database.DatabaseFactory
import ru.magnum.data.table.AlbumsTable
import ru.magnum.data.table.ArtistsTable
import ru.magnum.data.table.GenresTable
import ru.magnum.domain.model.Album
import ru.magnum.domain.model.Artist
import ru.magnum.domain.model.Genre
import java.time.LocalDate

data class Reference(
    val artists: List<Artist>,
    val albums: List<Album>,
    val genres: List<Genre>
)

class ReferenceRepository(
    private val databaseFactory: DatabaseFactory
) {
    suspend fun getAll(): Reference {
        return databaseFactory.dbQuery {
            Reference(
                artists = ArtistsTable.selectAll()
                    .map { row ->
                        Artist(
                            id = row[ArtistsTable.id],
                            name = row[ArtistsTable.name]
                        )
                    }
                    .sortedBy { it.name }
                ,
                albums = AlbumsTable.selectAll()
                    .map { row ->
                        Album(
                            id = row[AlbumsTable.id],
                            title = row[AlbumsTable.title],
                            releaseDate = row[AlbumsTable.releaseDate]
                        )
                    }
                    .sortedBy { it.title }
                ,
                genres = GenresTable.selectAll()
                    .map { row ->
                        Genre(
                            id = row[GenresTable.id],
                            name = row[GenresTable.name]
                        )
                    }
                    .sortedBy { it.name }
            )
        }
    }

    suspend fun createArtist(name: String): Artist {
        return databaseFactory.dbQuery {
            val exists = ArtistsTable.selectAll().where { ArtistsTable.name eq name }.any()
            require(!exists) {
                "Artist already exists"
            }
            val id = ArtistsTable.insert {
                it[ArtistsTable.name] = name
            } get ArtistsTable.id
            Artist(id, name)
        }
    }

    suspend fun createAlbum(title: String, releaseDate: LocalDate?): Album {
        return databaseFactory.dbQuery {
            val id = AlbumsTable.insert {
                it[AlbumsTable.title] = title
                it[AlbumsTable.releaseDate] = releaseDate
            } get AlbumsTable.id
            Album(id, title, releaseDate)
        }
    }

    suspend fun createGenre(name: String): Genre {
        return databaseFactory.dbQuery {
            val exists = GenresTable.selectAll().where { GenresTable.name eq name }.any()
            require(!exists) {
                "Genre already exists"
            }
            val id = GenresTable.insert {
                it[GenresTable.name] = name
            } get GenresTable.id
            Genre(id, name)
        }
    }
}