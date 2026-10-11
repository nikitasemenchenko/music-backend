package ru.magnum.di

import org.koin.dsl.module
import ru.magnum.data.media.TrackAudioProvider
import ru.magnum.data.repository.TrackMediaRepositoryImpl
import ru.magnum.data.repository.TrackRepositoryImpl
import ru.magnum.domain.repository.TrackMediaRepository
import ru.magnum.domain.repository.TrackRepository
import ru.magnum.domain.usecase.GetTrackByIdUseCase
import ru.magnum.domain.usecase.GetTrackCoverUseCase
import ru.magnum.domain.usecase.GetTracksUseCase

val appModule = module {
    single<TrackRepository> {
        TrackRepositoryImpl(
            databaseFactory = get()
        )
    }

    single<TrackMediaRepository> {
        TrackMediaRepositoryImpl(
            databaseFactory = get(),
            fileStorage = get()
        )
    }

    single {
        TrackAudioProvider(
            databaseFactory = get(),
            fileStorage = get()
        )
    }

    factory {
        GetTrackCoverUseCase(
            repository = get(),
        )
    }

    factory {
        GetTracksUseCase(
            repository = get()
        )
    }
    factory {
        GetTrackByIdUseCase(
            repository = get()
        )
    }
}