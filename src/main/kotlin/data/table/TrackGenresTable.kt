package ru.magnum.data.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object TrackGenresTable : Table("track_genres") {
    val trackId = reference(
        name = "track_id",
        refColumn = TracksTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    val genreId = reference(
        name = "genre_id",
        refColumn = GenresTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    override val primaryKey = PrimaryKey(trackId, genreId)
}