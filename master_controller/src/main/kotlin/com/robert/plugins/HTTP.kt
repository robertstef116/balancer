package com.robert.plugins

import io.ktor.http.*
import io.ktor.features.*
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*

fun Application.configureHTTP() {
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Put)
        method(HttpMethod.Post)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        allowHeaders { true }
        header(HttpHeaders.Authorization)
        header(HttpHeaders.ContentType)
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }
}
