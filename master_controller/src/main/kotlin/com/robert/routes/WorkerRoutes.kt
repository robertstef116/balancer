package com.robert.routes

import com.robert.WorkerCreateRequest
import com.robert.WorkerUpdateRequest
import com.robert.services.WorkerService
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.worker(path: String) {
    val workerService = WorkerService()

    route(path) {
        get("/version") {
            call.respond(workerService.getCurrentVersion())
        }

        get("/{id}") {
            val id = call.parameters["id"].toString()
            call.respond(workerService.get(id))
        }

        get {
            call.respond(workerService.getAll())
        }

        post {
            val request = call.receive<WorkerCreateRequest>()
            call.respond(workerService.add(request.alias.trim(), request.host.trim(), request.inUse))
        }

        put("/{id}") {
            val id = call.parameters["id"].toString()
            val request = call.receive<WorkerUpdateRequest>()
            call.respond(workerService.update(id, request.alias?.trim(), request.inUse))
        }

        delete("/{id}") {
            val id = call.parameters["id"].toString()
            call.respond(workerService.delete(id))
        }
    }
}
