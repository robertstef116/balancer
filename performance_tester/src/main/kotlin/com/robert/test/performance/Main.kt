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
import kotlin.random.Random
import kotlin.system.measureTimeMillis

val NO_REQUESTS_IN_BATCH = Env.getInt("NO_REQUESTS_IN_BATCH", 100)
val NO_BATCHES = Env.getInt("NO_BATCHES", 2)
val BASE_URL = Env.get("BASE_URL", "http://192.168.56.102:32783/test")
//val BASE_URL = Env.get("BASE_URL", "http://localhost:9990/internal/test")
val RESULTS_BASE_PATH = Env.get("RESULTS_PATH", ".")

var TEST_COUNT = 0

fun main() {
    val time = measureTimeMillis {
        executeTest(100, 0)
//        executeTest(50, 5000)
//        executeTest(2000, 0)
    }
    println("Test done in $time ms")
}

fun executeTest(baseTime: Long, timeDelta: Long) {
    TEST_COUNT++
    val data = MutableList<ReqData?>(NO_REQUESTS_IN_BATCH * NO_BATCHES) { null }
    val failed = AtomicInteger(0)

    runBlocking {
        for (i in 0..<NO_BATCHES) {
            val requests = mutableListOf<Job>()
            for (j in 0..<NO_REQUESTS_IN_BATCH) {
                val idx = i * NO_REQUESTS_IN_BATCH + j
                requests.add(launch(Dispatchers.IO) {
                    try {
                        val expectedTime = baseTime + if (timeDelta > 0) Random.nextLong(timeDelta) else 0L
                        val actualTime = measureTimeMillis {
                            val res = HttpClient.get("$BASE_URL/$expectedTime")
                            if (res.status != HttpStatusCode.OK) {
                                throw IOException("$idx - ${res.status}")
                            }
                        }
                        data[idx] = ReqData(idx, System.currentTimeMillis(), expectedTime, actualTime, "")
                    } catch (e: Exception) {
                        println("Error " + e.message)
                        failed.getAndIncrement()
                        data[idx] = ReqData(idx, -1, -1, -1, e.message ?: "")
                    }
                })
            }

            requests.joinAll()
            println("Batch $i done.")
        }
    }

    println("Batches finished for test $TEST_COUNT, writing results.")

    val resultPath = "$RESULTS_BASE_PATH/test_$TEST_COUNT.csv"
    val file = File(resultPath)
    if (file.exists() && file.isFile) {
        file.delete()
    }
    BufferedWriter(FileWriter(resultPath, true)).use { fileWriter ->
        fileWriter.write("idx,expectedTime,actualTime,overheadTime,err\n")
        for (res in data.filterNotNull().sortedBy { it.time }) {
            fileWriter.write("${res.idx},${res.expectedTime},${res.actualTime},${res.actualTime - res.expectedTime},${res.err}\n")
        }
    }

    println(failed.get().toString() + " requests failed.")
    println("---------------------------")
}

data class ReqData(
    val idx: Int,
    val time: Long,
    val expectedTime: Long,
    val actualTime: Long,
    val err: String
)
