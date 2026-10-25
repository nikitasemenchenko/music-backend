package ru.magnum.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.magnum.api.routes.adminReferenceRoutes
import ru.magnum.api.routes.adminTrackRoutes
import ru.magnum.api.routes.trackRoutes

fun Application.configureRouting() {
    routing {
        get("/health/live") {
            call.respondText("OK")
        }
        staticResources("/admin", "admin")
        trackRoutes()
        adminTrackRoutes()
        adminReferenceRoutes()
    }
}