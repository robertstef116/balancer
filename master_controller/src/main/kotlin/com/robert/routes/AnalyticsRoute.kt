package com.robert.routes

import com.robert.AnalyticsEntry
import com.robert.services.AnalyticsService
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

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
