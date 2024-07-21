package com.robert.api.routes

import com.robert.api.model.workflow.WorkflowCreateRequest
import com.robert.api.model.workflow.WorkflowUpdateRequest
import com.robert.api.service.WorkflowService
import com.robert.exceptions.NotFoundException
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.workflow(path: String, workflowService: WorkflowService) {
    route(path) {
        get("/{id}") {
            val id = UUID.fromString(call.parameters["id"].toString())
            call.respond(workflowService.get(id) ?: throw NotFoundException())
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
                    request.cpuLimit,
                    request.minDeployments,
                    request.maxDeployments,
                    request.algorithm,
                    request.pathMapping
                )
            )
        }

        put("/{id}") {
            val id = UUID.fromString(call.parameters["id"].toString())
            val request = call.receive<WorkflowUpdateRequest>()
            call.respond(
                workflowService.update(
                    id,
                    request.minDeployments,
                    request.maxDeployments,
                    request.algorithm
                )
            )
        }

        delete("/{id}") {
            val id = UUID.fromString(call.parameters["id"].toString())
            call.respond(workflowService.delete(id))
        }
    }
}
