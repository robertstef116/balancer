package com.robert.routes

import com.robert.DockerCreateContainerRequest
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

        post {
            val createContainerRequest = call.receive(DockerCreateContainerRequest::class)
            call.respond(dockerService.startContainer(createContainerRequest))
        }

        delete("/{id}") {
            val id = call.parameters["id"].toString()

            dockerService.removeContainer(id)
            call.respond(HttpStatusCode.OK)
        }
    }
}
