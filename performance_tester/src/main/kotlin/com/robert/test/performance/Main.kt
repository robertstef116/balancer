package com.robert.test.performance

import com.robert.Env
import com.robert.HttpClient
import io.ktor.http.*
import kotlinx.coroutines.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

val NO_REQUESTS_IN_BATCH = Env.getInt("NO_REQUESTS_IN_BATCH", 400)
val NO_BATCHES = Env.getInt("NO_BATCHES", 300)
val URL = Env.get("URL", "http://localhost:32778/test/1000?delta=0")
val RESULTS_PATH = Env.get("RESULTS_PATH", "./test.csv")

data class ReqData(val idx: Int, val time: Long, val err: String?)

fun main() {
    val data = MutableList<ReqData?>(NO_REQUESTS_IN_BATCH * NO_BATCHES) { null }
    val failed = AtomicInteger(0)

    runBlocking {
        for (i in 0..<NO_BATCHES) {
            val requests = mutableListOf<Job>()
            for (j in 0..<NO_REQUESTS_IN_BATCH) {
                val idx = i * NO_REQUESTS_IN_BATCH + j
                requests.add(launch(Dispatchers.IO) {
                    try {
                        val time = measureTimeMillis {
                            val res = HttpClient.get(URL)
                            if (res.status != HttpStatusCode.OK) {
                                throw IOException("$idx - ${res.status}")
                            }
                        }
                        data[idx] = ReqData(idx, time, "")
                    } catch (e: Exception) {
                        println("Error " + e.message)
                        failed.getAndIncrement()
                        data[idx] = ReqData(idx, -1, e.message)
                    }
                })
            }

            requests.joinAll()
            println("Batch $i done.")
        }
    }

    println("Batches finished, writing results.")

    val file = File(RESULTS_PATH)
    if (file.exists() && file.isFile) {
        file.delete()
    }
    BufferedWriter(FileWriter(RESULTS_PATH, true)).use {
        it.write("idx,time,err\n")
        for (res in data) {
            it.write("${res!!.idx},${res.time},${res.err}\n")
        }
    }

    println(failed.get().toString() + " requests failed.")
}
