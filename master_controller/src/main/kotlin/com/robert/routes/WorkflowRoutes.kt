package com.robert.routes

import com.robert.api.request.WorkflowCreateRequest
import com.robert.api.request.WorkflowUpdateRequest
import com.robert.services.WorkflowService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.workflow(path: String, workflowService: WorkflowService) {
    route(path) {
        get("/{id}") {
            val id = call.parameters["id"].toString()
            call.respond(workflowService.get(id))
        }

        get {
            call.respond(workflowService.getAll())
        }

        post {
            val request = call.receive<WorkflowCreateRequest>()
            call.respond(
                workflowService.add(
                    request.image.trim(),
                    request.memoryLimit,
                    request.minDeployments,
                    request.maxDeployments,
                    request.upScaling,
                    request.upScaling,
                    request.algorithm,
                    request.pathMapping
                )
            )
        }

        put("/{id}") {
            val id = call.parameters["id"].toString()
            val request = call.receive<WorkflowUpdateRequest>()
            call.respond(
                workflowService.update(
                    id,
                    request.minDeployments,
                    request.maxDeployments,
                    request.upScaling,
                    request.downScaling,
                    request.algorithm
                )
            )
        }

        delete("/{id}") {
            val id = call.parameters["id"].toString()
            call.respond(workflowService.delete(id))
        }
    }
}
