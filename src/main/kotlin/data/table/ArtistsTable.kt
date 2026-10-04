package ru.magnum.data.table

import org.jetbrains.exposed.v1.core.Table

object ArtistsTable : Table("artists") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    override val primaryKey = PrimaryKey(id)
}