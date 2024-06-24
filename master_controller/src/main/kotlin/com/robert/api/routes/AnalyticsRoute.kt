package com.robert.api.routes

import com.robert.api.service.AnalyticsService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.analytics(path: String, analyticsService: AnalyticsService) {
    route(path) {
        get("/requests") {
            val from = call.request.queryParameters["from"]
            val workerId = call.request.queryParameters["workerId"]
            val workflowId = call.request.queryParameters["workflowId"]
            val deploymentId = call.request.queryParameters["deploymentId"]

            call.respond(analyticsService.getAnalytics(from, workerId, workflowId, deploymentId))
        }

        get("/scaling") {
            val from = call.request.queryParameters["from"]
            val workerId = call.request.queryParameters["workerId"]
            val workflowId = call.request.queryParameters["workflowId"]

            val (workflowCurrentScaling, analyticsData) = analyticsService.getWorkflowAnalytics(from, workerId, workflowId)

            call.respond(
                mapOf(
                    "analyticsData" to analyticsData,
                    "currentScaling" to workflowCurrentScaling
                )
            )
        }
    }
}
