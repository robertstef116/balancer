package com.robert.plugins

import com.robert.exceptions.AuthenticationException
import com.robert.exceptions.AuthorizationException
import com.robert.routes.docker
import com.robert.routes.resource
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.features.*
import io.ktor.application.*
import io.ktor.response.*

fun Application.configureRouting() {
    routing {
//        authenticate("digest_auth") {
        docker()
        resource()
//        }

        get("/health") {
            call.respondText("healthy")
        }

        install(StatusPages) {
            exception<NotFoundException> {
                call.respond(HttpStatusCode.NotFound)
            }
            exception<AuthenticationException> {
                call.respond(HttpStatusCode.Unauthorized)
            }
            exception<AuthorizationException> {
                call.respond(HttpStatusCode.Forbidden)
            }

        }
    }
}
