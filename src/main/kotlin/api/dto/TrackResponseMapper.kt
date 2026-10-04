package ru.magnum.api.dto

import ru.magnum.domain.model.Track

fun Track.toResponse(): TrackResponse {
    return TrackResponse(
        id = id,
        title = title,
        artists = artists.map {
            ArtistResponse(
                id = it.id,
                name = it.name
            )
        },
        album = album?.let {
            AlbumResponse(
                id = it.id,
                title = it.title
            )
        },
        genres = genres.map {
            GenreResponse(
                id = it.id,
                name = it.name
            )
        },
        duration = durationMs,
        releaseYear = releaseYear
    )
}