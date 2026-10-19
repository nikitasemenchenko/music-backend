package ru.magnum.domain.model

import java.time.LocalDate

data class UpdateTrack(
    val title: String,
    val artistIds: List<Int>,
    val albumIds: List<Int>,
    val genreIds: List<Int>,
    val durationMs: Int,
    val releaseDate: LocalDate?
)
