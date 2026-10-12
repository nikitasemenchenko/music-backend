package ru.magnum.domain.model

data class CreateTrack(
    val title: String,
    val artistIds: List<Int>,
    val albumId: Int?,
    val genreIds: List<Int>,
    val durationMs: Int,
    val releaseYear: Short?
)
