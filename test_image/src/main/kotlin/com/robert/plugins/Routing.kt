package com.robert.plugins

import com.robert.exceptions.AuthenticationException
import com.robert.exceptions.AuthorizationException
import com.robert.mainRoute
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {


    routing {
        mainRoute("/busy")
        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }
    }

    install(StatusPages) {
        exception<AuthenticationException> { call, _ ->
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { call, _ ->
            call.respond(HttpStatusCode.Forbidden)
        }

    }
}
