package ru.magnum.api.dto

import kotlinx.serialization.Serializable
import ru.magnum.api.serialization.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class TrackResponse(
    val id: Int,
    val title: String,
    val artists: List<ArtistResponse>,
    val albums: List<AlbumResponse>,
    val genres: List<GenreResponse>,
    val durationMs: Int,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate?,
    val coverUrl: String?,
    val streamUrl: String
)