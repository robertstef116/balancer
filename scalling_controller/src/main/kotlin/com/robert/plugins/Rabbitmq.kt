package com.robert.plugins
import com.robert.RabbitmqService
import io.ktor.server.application.Application
import org.koin.ktor.ext.get

fun Application.configureRabbitmq() {
    val rabbitmqService: RabbitmqService = get()
    rabbitmqService.createConsumers()
}
