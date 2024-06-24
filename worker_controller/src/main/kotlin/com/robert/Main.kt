package com.robert

import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import com.robert.service.ResourceService
import com.robert.service.DockerService
import com.robert.controller.Controller
import com.robert.scaling.client.ScalingClient

suspend fun main() {
    // fix for spotify docker-client to get the right docker socket url
    if (System.getProperty("os.name") == "Windows 11") {
        val systemProperties = System.getProperties()
        systemProperties["os.name"] = "Windows 10"
    }

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

    app.koin.get<ScalingClient>()
        .connect()

    val schedulerService = app.koin.get<SchedulerService>()
    println(app.koin.get<ResourceService>().getResources())
    schedulerService.createSchedulers()
    schedulerService.wait()
//    scalingClient.close() ??
}
