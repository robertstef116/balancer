package com.robert.routes

import com.robert.services.ConfigService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.config(path: String, configService: ConfigService) {
    route(path) {
        get {
            val configs = configService.getConfigs()
            call.respond(configs)
        }
        put {
            val request = call.receive<Map<String, String>>()
            configService.setConfig(request)
            call.respond(HttpStatusCode.OK)
        }
    }
}
