package ru.magnum.domain.model

import java.time.OffsetDateTime

data class Track(
    val id: Int,
    val title: String,
    val artists: List<Artist>,
    val album: Album?,
    val genres: List<Genre>,
    val durationMs: Int,
    val releaseYear: Short?,
    val createdAt: OffsetDateTime
)