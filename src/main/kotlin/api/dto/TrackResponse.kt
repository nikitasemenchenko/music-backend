package ru.magnum.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class TrackResponse(
    val id: Int,
    val title: String,
    val artists: List<ArtistResponse>,
    val album: AlbumResponse?,
    val genres: List<GenreResponse>,
    val duration: Int,
    val releaseYear: Short?
)