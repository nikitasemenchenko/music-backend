package ru.magnum.api.routes

import io.ktor.server.application.ApplicationCall

fun ApplicationCall.trackIdOrNull(): Int? {
    return parameters["id"]?.toIntOrNull()?.takeIf { it > 0 }
}