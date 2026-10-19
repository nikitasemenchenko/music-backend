package ru.magnum.api.dto

import kotlinx.serialization.Serializable
import ru.magnum.api.serialization.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class CreateTrackRequest(
    val title: String,
    val artistIds: List<Int>,
    val albumIds: List<Int>,
    val genreIds: List<Int>,
    val durationMs: Int,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate?
)