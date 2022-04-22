package com.robert.plugins

import com.robert.exceptions.*
import com.robert.exceptions.NotFoundException
import com.robert.routes.workflow
import com.robert.routes.login
import com.robert.routes.worker
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*

fun Application.configureRouting() {
    routing {
        login()

//        authenticate("auth-jwt") {
        worker("/worker")
        workflow("/workflow")
//        }

        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }

        install(StatusPages) {
            exception<ServerException> {
                call.respond(HttpStatusCode.InternalServerError)
            }
            exception<ValidationException> {
                call.respondText(it.message, ContentType.Text.Plain, HttpStatusCode.BadRequest)
            }
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
