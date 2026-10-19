package ru.magnum.data.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object TracksTable : Table("tracks") {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 255)
    val durationMs = integer("duration_ms")
    val releaseDate = date("release_date").nullable()
    val audioPath = text("audio_path").uniqueIndex()
    val coverPath = text("cover_path").nullable()
    val createdAt = timestampWithTimeZone("created_at").databaseGenerated()
    override val primaryKey = PrimaryKey(id)
}