package com.robert

import com.robert.persistence.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.robert.plugins.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init(Workers, Workflows, WorkflowPathsMapping, Deployments, DeploymentPortsMapping)
    configureHTTP()
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureRouting()
    configureKoin()
    configureSchedules()
    configureRabbitmq()
}
