package com.robert.test.image.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
import kotlin.system.measureTimeMillis

fun Application.configureRouting() {
    routing {
        val requestsCount = AtomicLong(0)

        get("/test/{milliseconds}") {
            val elapsedTime = measureTimeMillis {
                val milliseconds = call.parameters["milliseconds"]?.toInt() ?: 0
                val delta = call.request.queryParameters["delta"]?.toInt() ?: 0
                val sleepTime = milliseconds + if (delta > 0) Random.nextInt(delta) else 0
                val startTime = System.currentTimeMillis()
                while ((System.currentTimeMillis() - startTime) < sleepTime) {
                    delay(10)
                }
            }

            println("Request ${requestsCount.incrementAndGet()} spend $elapsedTime ms")
            call.respondText("Done in $elapsedTime ms.")
        }
    }
}
