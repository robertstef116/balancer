package com.robert

import com.robert.controller.Controller
import com.robert.scaling.client.ScalingClient
import com.robert.service.DockerService
import com.robert.service.ResourceService
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

suspend fun main() {
    val app = startKoin {
        modules(
            module {
                singleOf(::DockerService)
                singleOf(::SchedulerService)
                singleOf(::Controller)
                singleOf(::ResourceService)
                singleOf(::ScalingClient)
            }
        )
    }

    app.koin.get<DockerService>()
        .pingDocker()

    val scalingClient = app.koin.get<ScalingClient>()
        .connect()

    val schedulerService = app.koin.get<SchedulerService>()
    schedulerService.createSchedulers()
    schedulerService.wait()
    scalingClient.close()
}
