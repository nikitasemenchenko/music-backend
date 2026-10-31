package ru.magnum.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.magnum.api.serialization.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class AlbumResponse(
    val id: Int,
    val title: String,

    @SerialName("release_date")
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate?
)