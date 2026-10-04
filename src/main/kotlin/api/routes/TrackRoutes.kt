package ru.magnum.api.routes

import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import ru.magnum.api.dto.toResponse
import ru.magnum.domain.usecase.GetTracksUseCase

fun Route.trackRoutes() {
    val getTracksUseCase by inject<GetTracksUseCase>()

    route("api/v1/tracks") {
        get {
            val tracks = getTracksUseCase()
            call.respond(
                tracks.map {
                    it.toResponse()
                }
            )
        }
    }
}