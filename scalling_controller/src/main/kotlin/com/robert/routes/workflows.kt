package com.robert.routes

import com.robert.controller.ScalingManager
import com.robert.requests.CreateWorkflowRequest
import com.robert.requests.UpdateWorkflowRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import java.util.*

// TODO: validations
fun Route.workflows(basePath: String) {
    val scalingManager: ScalingManager by inject()

    route(basePath){
        get {
            call.respond(scalingManager.getWorkflows())
        }

        post {
            val req = call.receive(CreateWorkflowRequest::class)
            scalingManager.createWorkflow(req.image, req.memoryLimit, req.cpuLimit, req.minDeployments, req.maxDeployments, req.algorithm, req.pathsMapping)
            call.respond(HttpStatusCode.Created)
        }

        put("/{id}") {
            val req = call.receive(UpdateWorkflowRequest::class)
            val id = UUID.fromString(call.parameters["id"].toString())
            scalingManager.updateWorkflow(id, req.minDeployments, req.maxDeployments, req.algorithm)
            call.respond(HttpStatusCode.OK)
        }

        delete("/{id}") {
            val id = UUID.fromString(call.parameters["id"].toString())
            scalingManager.deleteWorkflow(id)
            call.respond(HttpStatusCode.OK)
        }

        get("/loads") {
            call.respond(scalingManager.getResourcesLoad())
        }
    }
}
