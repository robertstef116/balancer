package com.robert.routes

import com.robert.DockerContainerStats
import com.robert.ResourcesInfo
import com.robert.WorkerResourceResponse
import com.robert.services.DockerService
import com.robert.services.ResourceService
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.resource() {
    val resourceService = ResourceService()
    val dockerService = DockerService()

    route("/resource") {
        get {
            call.respond(WorkerResourceResponse(resourceService.getResources(), dockerService.managedContainersStats()))
        }
    }
}
