package ru.magnum.data.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date

object AlbumsTable : Table("albums") {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 255)
    val releaseDate = date("release_date").nullable()
    val coverPath = text("cover_path").nullable()
    override val primaryKey = PrimaryKey(id)
}