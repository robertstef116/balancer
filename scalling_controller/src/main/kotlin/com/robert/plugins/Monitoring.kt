package com.robert.plugins

import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.application.*
import org.slf4j.event.Level

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
}
