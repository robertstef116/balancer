package com.robert.routes

import com.robert.services.DeploymentService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.deployment(path: String, deploymentService: DeploymentService) {
    route(path) {
        get {
            call.respond(deploymentService.getAll())
        }
    }
}
