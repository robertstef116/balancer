package com.robert.test.performance

import com.robert.Env
import com.robert.HttpClient
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class LoadGenerator {
    private val GENERATOR_BASE_PATH = Env.get("GENERATOR_BASE_PATH", "http://172.24.251.255:9990")
    private val GENERATOR_ROUTES = Env.get("GENERATOR_ROUTES", "/internal,/todos,/todos-api,/aws,/simple-app")
    private val GENERATOR_SPEED = Env.getInt("GENERATOR_SPEED", 5)

    fun execute() {
        val routes = GENERATOR_ROUTES.split(",")
        val activeRequests = AtomicInteger()
        var i = 0
        runBlocking {
            while (true) {
                println("new request ${i++}..")
                if (activeRequests.get() < 10) {
                    activeRequests.incrementAndGet()
                    launch(Dispatchers.IO) {
                        val route = routes[Random.nextInt(routes.size)]
                        val res = HttpClient.get(GENERATOR_BASE_PATH + route)
                        if (res.status != HttpStatusCode.OK) {
                            println("Unexpected status on route $route - ${res.status}")
                        }
                        activeRequests.decrementAndGet()
                    }
                }
                delay(15000L / GENERATOR_SPEED + Random.nextInt(GENERATOR_SPEED) * 100)
            }
        }
    }
}
