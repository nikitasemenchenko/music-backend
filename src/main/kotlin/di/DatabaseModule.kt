package ru.magnum.di

import io.ktor.server.config.ApplicationConfig
import org.koin.dsl.module
import org.koin.dsl.onClose
import ru.magnum.data.database.DatabaseFactory
import ru.magnum.data.database.DatabaseSettings

fun databaseModule(
    config: ApplicationConfig
) = module {
    single {
        DatabaseSettings(
            jdbcUrl = config.property("database.jdbcUrl").getString(),
            username = config.property("database.username").getString(),
            password = config.property("database.password").getString(),
            driverClassName = config.property("database.driverClassName").getString(),
            maximumPoolSize = config.property("database.maximumPoolSize").getString().toInt()
        )
    }
    single(createdAtStart = true) {
        DatabaseFactory(
            settings = get<DatabaseSettings>()
        )
    } onClose {
        it?.close()
    }

    single {
        get<DatabaseFactory>().database
    }
}