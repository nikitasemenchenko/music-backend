package ru.magnum.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArtistResponse(
    val id: Int,
    val name: String
)