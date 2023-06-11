package com.robert.plugins
import com.robert.SchedulerService
import io.ktor.server.application.Application
import org.koin.ktor.ext.get

fun Application.configureSchedules() {
    val schedulerService: SchedulerService = get()
    schedulerService.createSchedulers()
}
