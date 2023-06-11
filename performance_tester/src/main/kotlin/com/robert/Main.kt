package com.robert

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedWriter
import java.io.FileWriter
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.random.Random

const val NO_REQS = 150
const val MAX_TIME = 5000L
const val LB_URL = "http://127.0.0.1:9999/testx/busy"
//const val LB_URL = " http://192.168.100.93:49166/busy"

data class ReqData(val start: Long, val end: Long, val time: Long, val err: String?)

fun main() {
    val times = List(NO_REQS) { abs(Random.nextInt() % MAX_TIME) }
    val data = MutableList<ReqData?>(NO_REQS) { null }
    val failed = AtomicInteger(0)

    runBlocking {
        for (i in times.indices) {
            launch(Dispatchers.IO) {
                val time = times[i]
                println("start $time")
                val start = System.nanoTime()
                try {
                    HttpClient.get("$LB_URL/$time", Long.MAX_VALUE)
                    val end = System.nanoTime()
                    data[i] = ReqData(start/1000000, end/1000000, time, null)
                    println("done $time")
                } catch (e: Exception) {
                    println("error " + e.message)
                    failed.getAndIncrement()
                    data[i] = ReqData(-1, -1, time, e.message)
                }
            }
        }
    }

    BufferedWriter(FileWriter("./test.csv", true)).use {
//        it.write("start,end,time,err\n")
        for (res in data) {
            it.write("${res!!.start},${res.end},${res.time},${res.err}\n")
        }
    }

    println(failed.get().toString() + " requests failed")
}
