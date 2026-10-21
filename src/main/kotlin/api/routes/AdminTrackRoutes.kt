package ru.magnum.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import ru.magnum.api.dto.CreateTrackRequest
import ru.magnum.api.dto.CreateTrackResponse
import ru.magnum.api.dto.ErrorResponse
import ru.magnum.api.dto.UpdateTrackRequest
import ru.magnum.api.validation.validateTrackMetadata
import ru.magnum.application.CreateTrackService
import ru.magnum.application.DeleteTrackService
import ru.magnum.application.UpdateTrackService
import ru.magnum.data.storage.FileStorage
import ru.magnum.domain.model.CreateTrack
import ru.magnum.domain.model.UpdateTrack

fun Route.adminTrackRoutes() {
    val createTrackService by inject<CreateTrackService>()
    val deleteTrackService by inject<DeleteTrackService>()
    val updateTrackService by inject<UpdateTrackService>()
    val fileStorage by inject<FileStorage>()

    route("/api/v1/admin/tracks") {

        post {
            var upload: TrackMultipartData? = null
            var tempFilesInUse = false

            try {
                val receivedUpload = call.receiveTrackMultipart(fileStorage)
                upload = receivedUpload

                val request = receivedUpload.metadataJson?.let {
                    Json.decodeFromString<CreateTrackRequest>(it)
                } ?: throw IllegalArgumentException("Metadata is required")

                validateTrackMetadata(
                    title = request.title,
                    artistIds = request.artistIds,
                    albumIds = request.albumIds,
                    genreIds = request.genreIds,
                    durationMs = request.durationMs
                )

                val audioFile = receivedUpload.audioTempFile
                    ?: throw IllegalArgumentException("Audio file is required")

                val audioExtension = receivedUpload.audioExtension
                    ?: throw IllegalArgumentException("Audio file is required")

                tempFilesInUse = true

                val id = createTrackService.createTrack(
                    track = CreateTrack(
                        title = request.title.trim(),
                        artistIds = request.artistIds.distinct(),
                        albumIds = request.albumIds.distinct(),
                        genreIds = request.genreIds.distinct(),
                        durationMs = request.durationMs,
                        releaseDate = request.releaseDate
                    ),
                    audioTempFile = audioFile,
                    audioExtension = audioExtension,
                    coverTempFile = receivedUpload.coverTempFile,
                    coverExtension = receivedUpload.coverExtension
                )

                call.respond(
                    status = HttpStatusCode.Created,
                    message = CreateTrackResponse(id = id)
                )
            } finally {
                if (!tempFilesInUse) {
                    upload?.audioTempFile?.let {
                        fileStorage.deleteTemp(it)
                    }

                    upload?.coverTempFile?.let {
                        fileStorage.deleteTemp(it)
                    }
                }
            }
        }


        delete("/{id}") {
            val id = call.trackIdOrNull()
            if (id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        "Invalid id"
                    )
                )
                return@delete
            }
            val deleted = deleteTrackService.deleteTrack(id)
            if (!deleted) {
                call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse(
                        "Track not found"
                    )
                )
                return@delete
            }
            call.respond(HttpStatusCode.NoContent)

        }


        put("/{id}") {
            val id = call.trackIdOrNull()

            if (id == null || id <= 0) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Invalid track id")
                )
                return@put
            }

            var upload: TrackMultipartData? = null
            var tempFilesInUse = false

            try {
                val receivedUpload = call.receiveTrackMultipart(fileStorage)
                upload = receivedUpload

                val request = receivedUpload.metadataJson?.let {
                    Json.decodeFromString<UpdateTrackRequest>(it)
                } ?: throw IllegalArgumentException("Metadata is required")

                validateTrackMetadata(
                    title = request.title,
                    artistIds = request.artistIds,
                    albumIds = request.albumIds,
                    genreIds = request.genreIds,
                    durationMs = request.durationMs
                )

                tempFilesInUse = true

                val updated = updateTrackService.updateTrack(
                    id = id,
                    track = UpdateTrack(
                        title = request.title.trim(),
                        artistIds = request.artistIds.distinct(),
                        albumIds = request.albumIds.distinct(),
                        genreIds = request.genreIds.distinct(),
                        durationMs = request.durationMs,
                        releaseDate = request.releaseDate
                    ),
                    audioTempFile = receivedUpload.audioTempFile,
                    audioExtension = receivedUpload.audioExtension,
                    coverTempFile = receivedUpload.coverTempFile,
                    coverExtension = receivedUpload.coverExtension
                )

                if (!updated) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse("Track not found")
                    )
                    return@put
                }

                call.respond(HttpStatusCode.NoContent)

            } finally {
                if (!tempFilesInUse) {
                    upload?.audioTempFile?.let {
                        fileStorage.deleteTemp(it)
                    }

                    upload?.coverTempFile?.let {
                        fileStorage.deleteTemp(it)
                    }
                }
            }
        }
    }
}