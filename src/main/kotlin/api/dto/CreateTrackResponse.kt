package ru.magnum.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateTrackResponse(
    val id: Int
)