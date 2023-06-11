package com.robert.routes

import com.robert.controller.ScalingManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.workflow(basePath: String) {
    val scalingManager: ScalingManager by inject()

    route(basePath){
        get {
            call.respond(scalingManager.getWorkflows())
        }

        post {

        }

        get("/load") {
            call.respond(scalingManager.getResourcesLoad())
        }
    }
}
