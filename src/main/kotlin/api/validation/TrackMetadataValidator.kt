package ru.magnum.api.validation

fun validateTrackMetadata(
    title: String,
    artistIds: List<Int>,
    albumIds: List<Int>,
    genreIds: List<Int>,
    durationMs: Int
) {
    val trimTitle = title.trim()

    require(trimTitle.isNotEmpty()) {
        "Title must not be blank"
    }

    require(
        trimTitle.codePointCount(
            0,
            trimTitle.length
        ) <= 255
    ) {
        "Title must be <= 255 characters"
    }

    require(artistIds.isNotEmpty()) {
        "At least one artist is required"
    }

    require(artistIds.all { it > 0 }) {
        "Artist IDs must be > 0"
    }

    require(albumIds.all { it > 0 }) {
        "Album IDs must be > 0"
    }

    require(genreIds.all { it > 0 }) {
        "Genre IDs must be > 0"
    }

    require(durationMs > 0) {
        "Duration must be > 0"
    }
}