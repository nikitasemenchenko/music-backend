package ru.magnum.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class AlbumResponse(
    val id: Int,
    val title: String
)