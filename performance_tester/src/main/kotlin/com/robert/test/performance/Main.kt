package com.robert.test.performance

import com.robert.Env

fun main() {
    when (Env.get("MODE", "asd")) {
        "GENERATOR" -> LoadGenerator().execute()
        else -> LoadTest().execute()
    }
}
