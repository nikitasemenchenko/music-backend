package ru.magnum.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.magnum.api.serialization.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class ReferencesResponse(
    val artists: List<ArtistResponse>,
    val albums: List<AlbumResponse>,
    val genres: List<GenreResponse>
)

@Serializable
data class CreateNameRequest(
    val name: String
)

@Serializable
data class CreateAlbumRequest(
    val title: String,

    @SerialName("release_date")
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate? = null
)