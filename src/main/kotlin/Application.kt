package ru.magnum

import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.ktor.plugin.Koin
import ru.magnum.di.appModule
import ru.magnum.di.databaseModule
import ru.magnum.plugins.configureRequestValidation
import ru.magnum.plugins.configureRouting
import ru.magnum.plugins.configureSerialization
import ru.magnum.plugins.configureStatusPages

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {

    install(Koin) {
        modules(
            appModule,
            databaseModule(
                environment.config
            )
        )
    }

    configureHttp()

    configureSerialization()

    configureRouting()

    configureRequestValidation()

    configureStatusPages()
}