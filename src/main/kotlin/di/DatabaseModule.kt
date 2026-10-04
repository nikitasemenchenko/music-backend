package ru.magnum.di

import org.koin.dsl.module
import org.koin.dsl.onClose
import ru.magnum.data.database.DatabaseFactory
import ru.magnum.data.database.DatabaseSettings

fun databaseModule(
    settings: DatabaseSettings
) = module {
    single {
        settings
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