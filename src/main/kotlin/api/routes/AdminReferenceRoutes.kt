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
import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.delete
import ru.magnum.api.dto.ErrorResponse
import ru.magnum.data.repository.DeleteReferenceResult
import java.sql.SQLException

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

        delete("/artists/{id}") {
            call.respondReferenceDeletion { id ->
                repository.deleteArtist(id)
            }
        }

        delete("/albums/{id}") {
            call.respondReferenceDeletion{ id ->
                repository.deleteAlbum(id)
            }
        }

        delete("/genres/{id}") {
            call.respondReferenceDeletion { id ->
                repository.deleteGenre(id)
            }
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

private suspend fun ApplicationCall.respondReferenceDeletion(
    operation: suspend (Int) -> DeleteReferenceResult
) {
    val id = parameters["id"]?.toIntOrNull()?.takeIf { it > 0 }

    if (id == null) {
        respond(
            HttpStatusCode.BadRequest,
            ErrorResponse("Invalid id")
        )
        return
    }

    val result = try {
        operation(id)
    } catch (e: Exception) {
        val isForeignKeyViolation =
            generateSequence<Throwable>(e) { it.cause }
                .filterIsInstance<SQLException>()
                .any { it.sqlState == "23503" }

        if (isForeignKeyViolation) {
            DeleteReferenceResult.IN_USE
        } else {
            throw e
        }
    }

    when (result) {
        DeleteReferenceResult.DELETED -> {
            respond(HttpStatusCode.NoContent)
        }

        DeleteReferenceResult.NOT_FOUND -> {
            respond(
                HttpStatusCode.NotFound,
                ErrorResponse("Not found")
            )
        }

        DeleteReferenceResult.IN_USE -> {
            respond(
                HttpStatusCode.Conflict,
                ErrorResponse("Used by tracks")
            )
        }
    }
}