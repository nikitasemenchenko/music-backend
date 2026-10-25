package ru.magnum.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import ru.magnum.data.repository.ReferenceRepository
import org.koin.ktor.ext.inject
import ru.magnum.api.dto.AlbumResponse
import ru.magnum.api.dto.ArtistResponse
import ru.magnum.api.dto.CreateAlbumRequest
import ru.magnum.api.dto.CreateNameRequest
import ru.magnum.api.dto.GenreResponse
import ru.magnum.api.dto.ReferencesResponse

fun Route.adminReferenceRoutes() {
    val repository by inject<ReferenceRepository>()

    route("/api/v1/admin/references") {
        get {
            val references = repository.getAll()

            call.respond(
                ReferencesResponse(
                    artists = references.artists.map {
                        ArtistResponse(it.id, it.name)
                    },
                    albums = references.albums.map {
                        AlbumResponse(it.id, it.title, it.releaseDate)
                    },
                    genres = references.genres.map {
                        GenreResponse(it.id, it.name)
                    }
                )
            )
        }

        post("/artists") {
            val request = call.receive<CreateNameRequest>()
            val name = validateName(request.name, 255)
            val artist = repository.createArtist(name)
            call.respond(
                HttpStatusCode.Created,
                ArtistResponse(artist.id, artist.name)
            )
        }
        post("/genres") {
            val request = call.receive<CreateNameRequest>()
            val name = validateName(request.name, 100)
            val genre = repository.createGenre(name)
            call.respond(
                HttpStatusCode.Created,
                ArtistResponse(genre.id, genre.name)
            )
        }
        post("/albums") {
            val request = call.receive<CreateAlbumRequest>()
            val title = validateName(request.title, 255)
            val album = repository.createAlbum(title, request.releaseDate)
            call.respond(
                HttpStatusCode.Created,
                AlbumResponse(album.id, album.title, album.releaseDate)
            )
        }

    }
}

private fun validateName(
    value: String,
    maxLength: Int
): String {
    val result = value.trim()
    require(result.isNotEmpty()){
        "Name must not be empty"
    }
    require(result.length <= maxLength) {
        "Name length must not be greater than $maxLength"
    }
    return result
}