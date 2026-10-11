package ru.magnum.di

import org.koin.dsl.module
import ru.magnum.data.storage.FileStorage
import ru.magnum.data.storage.StorageSettings

fun storageModule(
    settings: StorageSettings
) = module {
    single {
        settings
    }

    single {
        FileStorage(
            settings = get()
        )
    }
}