package com.robert.api

import com.robert.api.plugins.*
import com.robert.scaling.client.ScalingClient
import com.robert.storage.DatabaseInitializer
import com.robert.storage.repository.UserRepository
import com.robert.storage.repository.WorkerRepository
import com.robert.storage.repository.WorkflowRepository
import com.robert.storage.repository.exposed.UserRepositoryImpl
import com.robert.storage.repository.exposed.WorkerRepositoryImpl
import com.robert.storage.repository.exposed.WorkflowRepositoryImpl
import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun main(args: Array<String>) {
    val app = startKoin {
        modules(
            module {
                singleOf<UserRepository>(::UserRepositoryImpl)
                singleOf<WorkerRepository>(::WorkerRepositoryImpl)
                singleOf<WorkflowRepository>(::WorkflowRepositoryImpl)
                singleOf(::ScalingClient)
            }
        )
    }
    DatabaseInitializer.initialize()

    app.koin.get<ScalingClient>()
        .connect()

    EngineMain.main(args)
}

fun Application.module() {
    configureSecurity()
    configureRouting()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
}
