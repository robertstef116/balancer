package com.robert.test.image.plugins

import com.robert.Env
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

fun Application.configureRouting() {
    routing {
        val requestsCount = AtomicLong(0)
        val rootContext = Env.get("ROOT_CONTEXT", "/internal")

        route(rootContext) {
            get("/test/{milliseconds}") {
                val elapsedTime = measureTimeMillis {
                    val sleepTime = call.parameters["milliseconds"]?.toInt() ?: 0
                    val startTime = System.currentTimeMillis()
                    while ((System.currentTimeMillis() - startTime) < sleepTime) {
                        MutableList(100) { "x" }
                        delay(1)
                    }
                }

                println("Request ${requestsCount.incrementAndGet()} spend $elapsedTime ms")
                call.respondText("Done in $elapsedTime ms.")
            }
        }
    }
}
