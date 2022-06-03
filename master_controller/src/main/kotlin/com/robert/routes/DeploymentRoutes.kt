package com.robert.routes

import com.robert.services.DeploymentService
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.deployment(path: String, deploymentService: DeploymentService) {
    route(path) {
        get {
            call.respond(deploymentService.getAll())
        }
    }
}
