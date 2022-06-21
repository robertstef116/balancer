package com.robert.routes

import com.robert.api.request.LoginRequest
import com.robert.services.LoginService
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.login() {
    val loginService = LoginService()

    post("/login") {
        val loginRequest = call.receive<LoginRequest>()
        val token = loginService.login(loginRequest.username, loginRequest.password)
        call.respond(hashMapOf("token" to token))
    }
}
