package com.robert.plugins

import com.robert.exceptions.AuthenticationException
import com.robert.exceptions.AuthorizationException
import com.robert.exceptions.NotFoundException
import com.robert.routes.docker
import com.robert.routes.resource
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
//        authenticate("digest_auth") {
        docker()
        resource()
//        }

        get("/health") {
            call.respondText("healthy")
        }
    }

    install(StatusPages) {
        exception<NotFoundException> { call, _ ->
            call.respond(HttpStatusCode.NotFound)
        }
        exception<AuthenticationException> { call, _ ->
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { call, _ ->
            call.respond(HttpStatusCode.Forbidden)
        }

    }
}
