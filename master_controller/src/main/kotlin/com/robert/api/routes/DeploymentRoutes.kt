package com.robert.api.routes

import com.robert.api.service.DeploymentService
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
