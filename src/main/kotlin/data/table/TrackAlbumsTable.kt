package ru.magnum.data.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

object TrackAlbumsTable: Table("track_albums") {
    val trackId = reference(
        name = "track_id",
        refColumn = TracksTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    val albumId = reference(
        name = "album_id",
        refColumn = AlbumsTable.id,
        onDelete = ReferenceOption.RESTRICT
    )
    override val primaryKey = PrimaryKey(trackId, albumId)
}