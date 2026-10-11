package ru.magnum.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val message: String
)