package com.robert.routes

import com.robert.api.request.LoginRequest
import com.robert.services.LoginService
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.login() {
    val loginService = LoginService()

    post("/login") {
        val loginRequest = call.receive<LoginRequest>()
        val token = loginService.login(loginRequest.username, loginRequest.password)
        call.respond(hashMapOf("token" to token))
    }
}
