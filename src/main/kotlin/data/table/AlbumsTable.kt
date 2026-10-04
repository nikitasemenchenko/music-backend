package ru.magnum.data.table

import org.jetbrains.exposed.v1.core.Table

object AlbumsTable : Table("albums") {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 255)
    override val primaryKey = PrimaryKey(id)
}