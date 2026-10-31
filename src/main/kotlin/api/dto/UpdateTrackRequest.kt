package ru.magnum.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.magnum.api.serialization.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class UpdateTrackRequest(
    val title: String,

    @SerialName("artist_ids")
    val artistIds: List<Int>,

    @SerialName("album_ids")
    val albumIds: List<Int>,

    @SerialName("genre_ids")
    val genreIds: List<Int>,

    @SerialName("duration_ms")
    val durationMs: Int,

    @SerialName("release_date")
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate? = null
)
