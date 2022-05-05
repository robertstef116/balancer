package com.robert

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.delay

fun Route.mainRoute(path: String) {
    route(path) {
        get("/{milliseconds}") {
            val milliseconds = call.parameters["milliseconds"]?.toInt() ?: 0
            val sleepTime = milliseconds * 1000000L // convert to nanoseconds
            println("processing $milliseconds")
            val startTime = System.nanoTime()
            while ((System.nanoTime() - startTime) < sleepTime) {
                delay(100)
            }
            call.respond(HttpStatusCode.OK)
        }
    }
}
