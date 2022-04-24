package com.robert.routes

import com.robert.ConfigUpdateRequest
import com.robert.services.ConfigService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.config(path: String) {
    val configService = ConfigService()

    route(path) {
        put {
            val request = call.receive<ConfigUpdateRequest>()
            configService.setConfig(request.key.trim(), request.value.trim())
            call.respond(HttpStatusCode.OK)
        }
    }
}
