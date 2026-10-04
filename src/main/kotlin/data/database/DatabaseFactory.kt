package ru.magnum.data.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database

class DatabaseFactory(
    settings: DatabaseSettings
): AutoCloseable {
    private val dataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = settings.jdbcUrl
            username = settings.username
            password = settings.password
            maximumPoolSize = settings.maximumPoolSize
            driverClassName = settings.driverClassName
            poolName = "MusicDatabasePool"
            initializationFailTimeout = 5000L
        }
    )

    val database = Database.connect(dataSource)

    override fun close() {
        dataSource.close()
    }
}