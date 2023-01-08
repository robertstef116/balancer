package com.robert

import com.robert.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    // fix for spotify docker-client to get the right docker socket url
    if (System.getProperty("os.name") == "Windows 11") {
        val systemProperties = System.getProperties()
        systemProperties["os.name"] = "Windows 10"
    }

    configureRouting()
    configureSecurity()
    configureMonitoring()
    configureSerialization()
}
