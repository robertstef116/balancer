package com.robert

import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

suspend fun main() {
    startKoin {
        modules(module {
            singleOf(::Storage)
            singleOf(::PulsarService)
            singleOf(::SchedulerService)
            singleOf(::HealthChecker)
        })
    }

    Application().run()
}
