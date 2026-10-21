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
        albums = albums.map {
            AlbumResponse(
                id = it.id,
                title = it.title,
                releaseDate = it.releaseDate
            )
        },
        genres = genres.map {
            GenreResponse(
                id = it.id,
                name = it.name
            )
        },
        durationMs = durationMs,
        releaseDate = releaseDate
    )
}