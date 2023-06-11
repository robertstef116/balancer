package com.robert

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

object HttpClient {
    val client: io.ktor.client.HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
            install(HttpTimeout)
//        install(Logging) {
//            logger = Logger.EMPTY
//            level = LogLevel.INFO
//        }
        }
    }

    suspend inline fun get(url: String, timeout: Long = Long.MAX_VALUE): HttpResponse =
        this.client.get(url) {
            timeout {
                requestTimeoutMillis = timeout
            }
            headers {
                append(HttpHeaders.ContentType, "application/json")
            }
        }

    suspend inline fun post(url: String, reqBody: Any, timeout: Long = Long.MAX_VALUE): HttpResponse =
        this.client.post(url) {
            setBody(reqBody)
            timeout {
                requestTimeoutMillis = timeout
            }
            headers {
                append(HttpHeaders.ContentType, "application/json")
            }
        }

    suspend inline fun delete(url: String, timeout: Long = Long.MAX_VALUE): HttpResponse =
        this.client.delete(url) {
            timeout {
                requestTimeoutMillis = timeout
            }
            headers {
                append(HttpHeaders.ContentType, "application/json")
            }
        }

    inline fun <reified T> blockingPost(url: String, reqBody: Any, timeout: Long = Long.MAX_VALUE): T = runBlocking(Dispatchers.IO){
        post(url, reqBody, timeout).body()
    }

    fun blockingDelete(url: String, timeout: Long=Long.MAX_VALUE): HttpResponse = runBlocking(Dispatchers.IO) {
        delete(url, timeout)
    }
}
