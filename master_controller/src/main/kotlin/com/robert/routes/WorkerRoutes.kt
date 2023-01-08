package com.robert.routes

import com.robert.api.request.WorkerUpdateRequest
import com.robert.api.response.WorkerCreateRequest
import com.robert.services.WorkerService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.worker(path: String, workerService: WorkerService) {
    route(path) {
        get("/{id}") {
            val id = call.parameters["id"].toString()
            call.respond(workerService.get(id))
        }

        get {
            call.respond(workerService.getAll())
        }

        post {
            val request = call.receive<WorkerCreateRequest>()
            call.respond(workerService.add(request.alias.trim(), request.host.trim(), request.port))
        }

        put("/{id}/stop") {
            val id = call.parameters["id"].toString()
            call.respond(workerService.flipStatus(id))
        }

        put("/{id}") {
            val id = call.parameters["id"].toString()
            val request = call.receive<WorkerUpdateRequest>()
            call.respond(workerService.update(id, request.alias?.trim(), request.port))
        }

        delete("/{id}") {
            val id = call.parameters["id"].toString()
            call.respond(workerService.delete(id))
        }
    }
}
