package com.robert.routes

import com.robert.api.request.DockerCreateContainerRequest
import com.robert.services.DockerService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.docker() {
    val dockerService = DockerService()

    route("/docker") {
        get {
            call.respond(dockerService.getManagedContainers())
        }

        get("/ports") {
            call.respond(dockerService.managedContainersPorts())
        }

        post {
            val req = call.receive(DockerCreateContainerRequest::class)
            call.respond(dockerService.startContainer(req.deploymentId, req.image, req.memoryLimit, req.ports))
        }

        delete("/{id}") {
            val id = call.parameters["id"].toString()

            dockerService.removeContainer(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}
