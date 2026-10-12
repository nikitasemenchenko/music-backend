package ru.magnum.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import ru.magnum.api.dto.CreateTrackRequest
import ru.magnum.api.dto.CreateTrackResponse
import ru.magnum.api.dto.ErrorResponse
import ru.magnum.application.CreateTrackService
import ru.magnum.application.DeleteTrackService
import ru.magnum.data.storage.FileStorage
import ru.magnum.domain.model.CreateTrack
import java.nio.file.Path

fun Route.adminTrackRoutes() {
    val createTrackService by inject<CreateTrackService>()
    val deleteTrackService by inject<DeleteTrackService>()
    val fileStorage by inject<FileStorage>()

    route("/api/v1/admin/tracks") {
        post {
            var metadataJson: String? = null
            var audioTempFile: Path? = null
            var audioExtension: String? = null
            var coverTempFile: Path? = null
            var coverExtension: String? = null

            try {
                val multipart = call.receiveMultipart()
                multipart.forEachPart { part ->
                    try {
                        when (part) {
                            is PartData.FormItem -> {
                                if (part.name == "metadata") {
                                    metadataJson = part.value
                                }
                            }

                            is PartData.FileItem -> {
                                when (part.name) {
                                    "audio" -> {
                                        val extension = fileExtension(part.originalFileName)
                                        if (extension != "mp3") {
                                            throw IllegalArgumentException("File extension must be 'mp3'")
                                        }
                                        val tempFile = fileStorage.createTempFile(
                                            extension = extension
                                        )
                                        try {
                                            part.provider()
                                                .copyAndClose(
                                                    tempFile.toFile().writeChannel()
                                                )
                                        } catch (e: Exception) {
                                            fileStorage.deleteTemp(tempFile)
                                            throw e
                                        }
                                        audioTempFile = tempFile
                                        audioExtension = extension
                                    }

                                    "cover" -> {
                                        val extension = fileExtension(part.originalFileName)
                                        if (extension !in COVER_EXTENSIONS) {
                                            throw IllegalArgumentException(
                                                "Cover must be JPG, JPEG, PNG or WEBP"
                                            )
                                        }
                                        val tempFile = fileStorage.createTempFile(
                                            extension = extension
                                        )
                                        try {
                                            part.provider()
                                                .copyAndClose(
                                                    tempFile.toFile().writeChannel()
                                                )
                                        } catch (e: Exception) {
                                            fileStorage.deleteTemp(tempFile)
                                            throw e
                                        }
                                        coverTempFile = tempFile
                                        coverExtension = extension
                                    }
                                }
                            }

                            else -> Unit
                        }
                    } finally {
                        part.release()
                    }
                }
                val request = metadataJson?.let {
                    Json.decodeFromString<CreateTrackRequest>(it)
                } ?: throw IllegalArgumentException("Metadata is required")

                validateRequest(request)

                val audioFile = audioTempFile ?: throw IllegalArgumentException("Audio file is required")
                val id = createTrackService.createTrack(
                    track = CreateTrack(
                        title = request.title.trim(),
                        artistIds = request.artistIds.distinct(),
                        albumId = request.albumId,
                        genreIds = request.genreIds.distinct(),
                        durationMs = request.durationMs,
                        releaseYear = request.releaseYear
                    ),
                    audioTempFile = audioFile,
                    audioExtension = audioExtension!!,
                    coverTempFile = coverTempFile,
                    coverExtension = coverExtension
                )
                call.respond(
                    status = HttpStatusCode.Created,
                    message = CreateTrackResponse(
                        id = id
                    )
                )
            } catch (e: SerializationException) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = ErrorResponse(
                        message = "Invalid metadata format"
                    )
                )
            } catch (e: IllegalArgumentException) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = ErrorResponse(
                        message = e.message ?: "Bad request"
                    )
                )
            } finally {
                audioTempFile?.let {
                    fileStorage.deleteTemp(it)
                }
                coverTempFile?.let {
                    fileStorage.deleteTemp(it)
                }
            }
        }

        delete("/{id}"){
            val id = call.parameters["id"]?.toIntOrNull()
            if(id == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        "Invalid id"
                    )
                )
                return@delete
            }
            val deleted = deleteTrackService.deleteTrack(id)
            if(!deleted) {
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
    }
}

private fun fileExtension(
    fileName: String?
): String {
    if (fileName.isNullOrBlank()) {
        return ""
    }
    return fileName.substringAfterLast('.', "").lowercase()
}

private val COVER_EXTENSIONS =
    setOf(
        "jpg",
        "jpeg",
        "png",
        "webp"
    )

private fun validateRequest(request: CreateTrackRequest) {

    require(
        request.title.isNotBlank()
    ) {
        "Title must not be blank"
    }


    require(
        request.artistIds.isNotEmpty()
    ) {
        "At least one artist is required"
    }


    require(
        request.durationMs > 0
    ) {
        "Duration must be greater than zero"
    }
}