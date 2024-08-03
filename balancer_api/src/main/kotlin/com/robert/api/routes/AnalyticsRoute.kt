package com.robert.api.routes

import com.robert.api.service.AnalyticsService
import com.robert.balancing.LoadBalancerResponseType
import com.robert.exceptions.ValidationException
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*

fun Route.analytics(path: String, analyticsService: AnalyticsService) {
    route(path) {
        get("/scaling") {
            val workflowId = call.request.queryParameters["workflowId"]?.let { UUID.fromString(it) }
            val durationMs = (call.request.queryParameters["durationMs"])?.toLong() ?: throw ValidationException("Invalid time")
            val metric = (call.request.queryParameters["metric"]) ?: throw ValidationException("Metric not specified")

            call.respond(analyticsService.getScalingAnalytics(workflowId, metric, durationMs))
        }

        get("/balancing") {
            val workflowId = call.request.queryParameters["workflowId"]?.let { UUID.fromString(it) }
            val responseType = call.request.queryParameters["responseType"]?.let { LoadBalancerResponseType.valueOf(it) } ?: LoadBalancerResponseType.OK
            val balancingPath = call.request.queryParameters["path"]
            val durationMs = (call.request.queryParameters["durationMs"])?.toLong() ?: throw ValidationException("Invalid time")
            val metric = (call.request.queryParameters["metric"]) ?: throw ValidationException("Metric not specified")

            call.respond(analyticsService.getLoadBalancingAnalytics(workflowId, balancingPath, responseType, metric, durationMs))
        }
//        get("/requests") {
//            val from = call.request.queryParameters["from"]
//            val workerId = call.request.queryParameters["workerId"]
//            val workflowId = call.request.queryParameters["workflowId"]
//            val deploymentId = call.request.queryParameters["deploymentId"]
//
//            call.respond(analyticsService.getAnalytics(from, workerId, workflowId, deploymentId))
//        }
//
//        get("/scaling") {
//            val from = call.request.queryParameters["from"]
//            val workerId = call.request.queryParameters["workerId"]
//            val workflowId = call.request.queryParameters["workflowId"]
//
//            val (workflowCurrentScaling, analyticsData) = analyticsService.getWorkflowAnalytics(from, workerId, workflowId)
//
//            call.respond(
//                mapOf(
//                    "analyticsData" to analyticsData,
//                    "currentScaling" to workflowCurrentScaling
//                )
//            )
//        }
    }
}
