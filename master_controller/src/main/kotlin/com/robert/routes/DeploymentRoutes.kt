package com.robert.routes

import com.robert.DeploymentCreateRequest
import com.robert.services.DeploymentService
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.deployment(path: String) {
    val deploymentService = DeploymentService()

    route(path) {
        get("/version") {
            call.respond(deploymentService.getCurrentVersion())
        }

        get("/{id}") {
            val id = call.parameters["id"].toString()
            call.respond(deploymentService.get(id))
        }

        get {
            call.respond(deploymentService.getAll())
        }

        post {
            val request = call.receive<DeploymentCreateRequest>()
            call.respond(deploymentService.add(request.path.trim(), request.image.trim(), request.memoryLimit, request.ports))
        }

        put("/{id}") {
            val id = call.parameters["id"].toString()
            val request = call.receive<DeploymentCreateRequest>()
            call.respond(deploymentService.update(id, request.path.trim(), request.image.trim(), request.memoryLimit, request.ports))
        }

        delete("/{id}") {
            val id = call.parameters["id"].toString()
            call.respond(deploymentService.delete(id))
        }
    }
}
