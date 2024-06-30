package com.robert.loadbalancer

import com.robert.SchedulerService
import com.robert.scaling.client.ScalingClient
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

suspend fun main() {
    val app = startKoin {
        modules(
            module {
                singleOf(::LoadBalancer)
                singleOf(::RequestHandlerProvider)
                singleOf(::ScalingClient)
                singleOf(::SchedulerService)
            }
        )
    }

    app.koin.get<ScalingClient>()
        .connect()

    val schedulerService = app.koin.get<SchedulerService>()
//    schedulerService.createSchedulers()

    app.koin.get<LoadBalancer>().start()
//    schedulerService.wait()
}