package ru.magnum.data.database

import org.flywaydb.core.Flyway

object MigrationFactory {
    fun migrate(
        settings: DatabaseSettings
    ) {
        Flyway.configure()
            .dataSource(
                settings.jdbcUrl,
                settings.username,
                settings.password,
            )
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }
}