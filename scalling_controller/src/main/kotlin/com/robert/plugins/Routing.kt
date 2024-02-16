package com.robert.plugins

import com.robert.exceptions.NotFoundException
import com.robert.routes.workers
import com.robert.routes.workflows
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(StatusPages) {
        exception<NotFoundException> { call, _ ->
            call.respond(HttpStatusCode.NotFound)
        }
        exception<Throwable> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    routing {
        workers("/workers")
        workflows("/workflows")
    }
}
