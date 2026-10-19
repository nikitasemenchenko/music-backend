package ru.magnum.api.dto

import kotlinx.serialization.Serializable
import ru.magnum.api.serialization.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class AlbumResponse(
    val id: Int,
    val title: String,
    @Serializable(with = LocalDateSerializer::class)
    val releaseDate: LocalDate?
)