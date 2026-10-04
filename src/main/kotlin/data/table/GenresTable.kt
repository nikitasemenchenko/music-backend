package ru.magnum.data.table

import org.jetbrains.exposed.v1.core.Table

object GenresTable : Table("genres") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    override val primaryKey = PrimaryKey(id)
}