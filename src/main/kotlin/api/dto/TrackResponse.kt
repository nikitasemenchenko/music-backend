package ru.magnum.api.dto

import kotlinx.serialization.SerialName
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

    @SerialName("duration_ms")
    val durationMs: Int,

    @SerialName("release_date")
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate?,

    @SerialName("cover_url")
    val coverUrl: String?,

    @SerialName("stream_url")
    val streamUrl: String
)