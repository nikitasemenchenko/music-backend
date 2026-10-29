package ru.magnum.domain.model

import java.time.LocalDate
import java.time.OffsetDateTime

data class Track(
    val id: Int,
    val title: String,
    val artists: List<Artist>,
    val albums: List<Album>,
    val genres: List<Genre>,
    val hasCover: Boolean,
    val durationMs: Int,
    val releaseDate: LocalDate?,
    val createdAt: OffsetDateTime
)