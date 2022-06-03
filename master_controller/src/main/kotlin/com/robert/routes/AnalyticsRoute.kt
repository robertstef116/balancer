package com.robert.routes

import com.robert.AnalyticsEntry
import com.robert.services.AnalyticsService
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.analytics(path: String, analyticsService: AnalyticsService) {
    route(path) {
        get {
            val from = call.request.queryParameters["from"]
            val workerId = call.request.queryParameters["workerId"]
            val workflowId = call.request.queryParameters["workflowId"]
            val deploymentId = call.request.queryParameters["deploymentId"]

            call.respond(analyticsService.getAnalytics(from, workerId, workflowId, deploymentId))
        }
    }
}
