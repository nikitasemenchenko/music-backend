package ru.magnum.di

import org.koin.dsl.module
import ru.magnum.data.repository.TrackRepositoryImpl
import ru.magnum.domain.repository.TrackRepository
import ru.magnum.domain.usecase.GetTracksUseCase

val appModule = module {
    single<TrackRepository> {
        TrackRepositoryImpl(
            databaseFactory = get()
        )
    }

    factory {
        GetTracksUseCase(
            repository = get()
        )
    }
}