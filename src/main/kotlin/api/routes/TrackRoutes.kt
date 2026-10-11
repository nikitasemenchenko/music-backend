package ru.magnum.api.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import ru.magnum.api.dto.ErrorResponse
import ru.magnum.api.dto.toResponse
import ru.magnum.domain.usecase.GetTrackByIdUseCase
import ru.magnum.domain.usecase.GetTracksUseCase

fun Route.trackRoutes() {
    val getTracksUseCase by inject<GetTracksUseCase>()
    val getTrackByIdUseCase by inject<GetTrackByIdUseCase>()

    route("/api/v1/tracks") {
        get {
            val tracks = getTracksUseCase()
            call.respond(
                tracks.map {
                    it.toResponse()
                }
            )
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null || id <= 0) {
                call.respond(
                    status = HttpStatusCode.BadRequest,
                    message = ErrorResponse(
                        message = "Invalid track id"
                    )
                )
                return@get
            }
            val track = getTrackByIdUseCase(id)
            if (track == null) {
                call.respond(
                    status = HttpStatusCode.NotFound,
                    message = ErrorResponse(
                        message = "Track not found"
                    )
                )
                return@get
            }
            call.respond(
                track.toResponse()
            )
        }
    }
}