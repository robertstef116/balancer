package com.robert

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Application : KoinComponent {
    private val schedulerService: SchedulerService by inject()
    private val pulsarService: PulsarService by inject()

    suspend fun run() {
        pulsarService.createConsumers()
        schedulerService.createSchedulers()

        schedulerService.wait()
    }
}
