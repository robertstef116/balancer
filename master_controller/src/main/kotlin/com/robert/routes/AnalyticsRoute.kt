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
            if (from != null) {
                call.respond(analyticsService.getAnalytics(from.toLong()))
            } else {
                call.respond(emptyList<AnalyticsEntry>())
            }
        }
    }
}
