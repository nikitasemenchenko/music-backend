package ru.magnum.data.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object TrackArtistsTable: Table("track_artists") {
    val trackId = reference(
        name = "track_id",
        refColumn = TracksTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    val artistId = reference(
        name = "artist_id",
        refColumn = ArtistsTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    override val primaryKey = PrimaryKey(trackId, artistId)
}