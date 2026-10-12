package ru.magnum.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateTrackRequest(
    val title: String,
    val artistIds: List<Int>,
    val albumId: Int?,
    val genreIds: List<Int>,
    val durationMs: Int,
    val releaseYear: Short?
)