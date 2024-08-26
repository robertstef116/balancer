package com.robert.test.performance

import com.robert.Env
import com.robert.HttpClient
import com.robert.logger
import io.ktor.http.*
import kotlinx.coroutines.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random
import kotlin.system.measureTimeMillis

class LoadTest {
    companion object {
        private val LOG by logger()
    }

    private val ACTIVE_REQUESTS_LIMIT = Env.getInt("ACTIVE_REQUESTS_LIMIT", 230)
    private val TOTAL_REQUESTS_IN_TEST = Env.getInt("TOTAL_REQUESTS_IN_TEST", 30_000)
    private val WARMUP_REQUESTS_COUNT = Env.getInt("WARMUP_REQUESTS_COUNT", 2_000)
    private val DATA_PLOT_INTERVALS = Env.getInt("DATA_PLOT_INTERVALS", 250)

    //    private val TEST_BASE_URL = Env.get("TEST_BASE_URL", "http://172.24.251.255:32770/internal/test")
    private val TEST_BASE_URL = Env.get("TEST_BASE_URL", "http://172.24.251.255:9990/internal/test")
    private val TEST_RESULTS_BASE_PATH = Env.get("TEST_RESULTS_BASE_PATH", ".")
    private val activeRequestCount = AtomicLong(0)

    fun execute() {
        val time = measureTimeMillis {
//            executeTest(1, 500, 0)
//            executeTest(2, 500, 4500)
            executeTest(3, 2000, 500)
            executeTest(30, 2000, 500)
        }
        LOG.info("Test done in $time ms")
    }

    private fun executeTest(testCount: Int, baseTime: Long, timeDelta: Long) {
        val data = MutableList<ReqData?>(TOTAL_REQUESTS_IN_TEST) { null }
        val failed = AtomicInteger(0)

        runBlocking {
            val requests = mutableListOf<Job>()
            for (i in 0..<TOTAL_REQUESTS_IN_TEST + WARMUP_REQUESTS_COUNT) {
                while (activeRequestCount.get() > ACTIVE_REQUESTS_LIMIT) {
                    delay(50)
                }
                if (i % 5000 == 0) {
                    LOG.info("$i/$TOTAL_REQUESTS_IN_TEST")
                }
                activeRequestCount.incrementAndGet()
                requests.add(launch(Dispatchers.IO) {
                    try {
                        val expectedTime = baseTime + if (timeDelta > 0) Random.nextLong(timeDelta) else 0L
                        val actualTime = measureTimeMillis {
                            val res = HttpClient.get("$TEST_BASE_URL/$expectedTime")
                            if (res.status != HttpStatusCode.OK) {
                                throw IOException("$i - ${res.status}")
                            }
                        }
                        if (i >= WARMUP_REQUESTS_COUNT) {
                            data[i - WARMUP_REQUESTS_COUNT] = ReqData(System.currentTimeMillis(), expectedTime, actualTime)
                        }
                    } catch (e: Exception) {
                        LOG.info("Error " + e.message)
                        failed.getAndIncrement()
                        if (i >= WARMUP_REQUESTS_COUNT) {
                            data[i - WARMUP_REQUESTS_COUNT] = ReqData(-1, -1, -1)
                        }
                    }
                    activeRequestCount.decrementAndGet()
                })

            }
            requests.joinAll()
        }

        LOG.info("Requests finished for test $testCount, writing results.")

        val resultPath = "$TEST_RESULTS_BASE_PATH/test_$testCount.csv"
        val file = File(resultPath)
        if (file.exists() && file.isFile) {
            file.delete()
        }
        val step = TOTAL_REQUESTS_IN_TEST / DATA_PLOT_INTERVALS
        BufferedWriter(FileWriter(resultPath, true)).use { fileWriter ->
            fileWriter.write("overhead\n")
            var count = 0
            var avg = 0.0
            for (res in data.filterNotNull().sortedBy { it.time }) {
                if (count == step) {
                    fileWriter.write("${avg}\n")
                    count = 0
                    avg = 0.0
                }
                avg = (avg * count + res.actualTime - res.expectedTime) / (count + 1)
                count++
            }
            fileWriter.write("${avg}\n")
        }

        LOG.info(failed.get().toString() + " requests failed.")
        LOG.info("---------------------------")
    }

    data class ReqData(
        val time: Long,
        val expectedTime: Long,
        val actualTime: Long,
    )
}