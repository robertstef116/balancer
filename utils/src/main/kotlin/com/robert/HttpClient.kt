package com.robert

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.features.json.*
import io.ktor.http.*

object HttpClient {
    private val client = HttpClient(CIO) {
        install(JsonFeature)
        install(HttpTimeout)
//        install(Logging) {
//            logger = Logger.EMPTY
//            level = LogLevel.INFO
//        }
    }

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    suspend inline fun <reified T : Any> get(url: String, timeout: Long = Long.MAX_VALUE): T = this.client.get(url) {
        timeout {
            requestTimeoutMillis = timeout
        }
    }

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    suspend inline fun <reified T : Any> post(url: String, reqBody: Any, timeout: Long = Long.MAX_VALUE): T =
        this.client.post(url) {
            body = reqBody
            timeout {
                requestTimeoutMillis = timeout
            }
        }

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    suspend inline fun <reified T : Any> delete(url: String, timeout: Long = Long.MAX_VALUE): T =
        this.client.delete(url) {
            timeout {
                requestTimeoutMillis = timeout
            }
        }
}
