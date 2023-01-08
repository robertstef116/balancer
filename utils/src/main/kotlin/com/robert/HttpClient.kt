package com.robert

import com.robert.api.response.DockerCreateContainerResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*

object HttpClient {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation)
        install(HttpTimeout)
//        install(Logging) {
//            logger = Logger.EMPTY
//            level = LogLevel.INFO
//        }
    }

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    suspend inline fun <reified T : Any> get(url: String, timeout: Long = Long.MAX_VALUE): HttpResponse = this.client.get(url) {
        timeout {
            requestTimeoutMillis = timeout
        }
        headers {
            append(HttpHeaders.ContentType, "application/json")
        }
    }

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    suspend inline fun <reified T : Any> post(url: String, reqBody: Any, timeout: Long = Long.MAX_VALUE): DockerCreateContainerResponse? =
        this.client.post(url) {
            setBody(reqBody)
            timeout {
                requestTimeoutMillis = timeout
            }
            headers {
                append(HttpHeaders.ContentType, "application/json")
            }
        }.body()

    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
    suspend inline fun <reified T : Any> delete(url: String, timeout: Long = Long.MAX_VALUE): HttpResponse =
        this.client.delete(url) {
            timeout {
                requestTimeoutMillis = timeout
            }
            headers {
                append(HttpHeaders.ContentType, "application/json")
            }
        }
}
