package ru.magnum.config

import io.ktor.server.config.ApplicationConfig
import ru.magnum.data.database.DatabaseSettings

fun ApplicationConfig.databaseSettings(): DatabaseSettings {
    return DatabaseSettings(
        jdbcUrl = property("database.jdbcUrl").getString(),
        username = property("database.username").getString(),
        password = property("database.password").getString(),
        driverClassName = property("database.driverClassName").getString(),
        maximumPoolSize = property("database.maximumPoolSize").getString().toInt()
    )
}