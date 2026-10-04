package ru.magnum.data.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object TracksTable : Table("tracks") {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 255)
    val albumId = optReference(
        name = "album_id",
        refColumn = AlbumsTable.id,
        onDelete = ReferenceOption.SET_NULL
    )
    val durationMs = integer("duration_ms")
    val releaseYear = short("release_year").nullable()
    val audioPath = text("audio_path").uniqueIndex()
    val coverPath = text("cover_path").nullable()
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    override val primaryKey = PrimaryKey(id)
}