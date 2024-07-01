package com.robert

import com.robert.controller.ScalingController
import com.robert.controller.WorkerController
import com.robert.controller.WorkflowController
import com.robert.server.ScalingServer
import com.robert.storage.DatabaseInitializer
import com.robert.storage.repository.ScalingAnalyticRepository
import com.robert.storage.repository.WorkerRepository
import com.robert.storage.repository.WorkflowRepository
import com.robert.storage.repository.exposed.ScalingAnalyticRepositoryImpl
import com.robert.storage.repository.exposed.WorkerRepositoryImpl
import com.robert.storage.repository.exposed.WorkflowRepositoryImpl
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

suspend fun main() {
    val app = startKoin {
        modules(
            module {
                singleOf(::ScalingServer)
                singleOf(::ScalingController)
                singleOf(::SchedulerService)
                singleOf(::WorkerController)
                singleOf(::WorkflowController)
                singleOf<ScalingAnalyticRepository>(::ScalingAnalyticRepositoryImpl)
                singleOf<WorkerRepository>(::WorkerRepositoryImpl)
                singleOf<WorkflowRepository>(::WorkflowRepositoryImpl)
            }
        )
    }

    val server = app.koin.get<ScalingServer>()
    val schedulerService = app.koin.get<SchedulerService>()

    DatabaseInitializer.initialize()

    server.start()
    schedulerService.createSchedulers()
    server.blockUntilShutdown()
    schedulerService.wait()
}