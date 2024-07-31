package com.robert.api.routes

import com.robert.api.model.worker.WorkerUpdateRequest
import com.robert.api.service.WorkerService
import com.robert.exceptions.NotFoundException
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.worker(path: String, workerService: WorkerService) {
    route(path) {
        get("/{id}") {
            val id = UUID.fromString(call.parameters["id"].toString())
            call.respond(workerService.get(id) ?: throw NotFoundException())
        }

        get {
            call.respond(workerService.getAll())
        }

        put("/{id}") {
            val id = UUID.fromString(call.parameters["id"].toString())
            val request = call.receive<WorkerUpdateRequest>()
            call.respond(workerService.update(id, request.state))
        }

        delete("/{id}") {
            val id = UUID.fromString(call.parameters["id"].toString())
            call.respond(workerService.delete(id))
        }
    }
}
