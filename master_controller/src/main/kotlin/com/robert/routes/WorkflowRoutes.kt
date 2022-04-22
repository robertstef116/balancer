package com.robert.routes

import com.robert.WorkflowCreateRequest
import com.robert.services.WorkflowService
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.workflow(path: String) {
    val workflowService = WorkflowService()

    route(path) {
        get("/version") {
            call.respond(workflowService.getCurrentVersion())
        }

        get("/{id}") {
            val id = call.parameters["id"].toString()
            call.respond(workflowService.get(id))
        }

        get {
            call.respond(workflowService.getAll())
        }

        post {
            val request = call.receive<WorkflowCreateRequest>()
            call.respond(workflowService.add(request.path.trim(), request.image.trim(), request.memoryLimit, request.ports))
        }

        put("/{id}") {
            val id = call.parameters["id"].toString()
            val request = call.receive<WorkflowCreateRequest>()
            call.respond(workflowService.update(id, request.path.trim(), request.image.trim(), request.memoryLimit, request.ports))
        }

        delete("/{id}") {
            val id = call.parameters["id"].toString()
            call.respond(workflowService.delete(id))
        }
    }
}
