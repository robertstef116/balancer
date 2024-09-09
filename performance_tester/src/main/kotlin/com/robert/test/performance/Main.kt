package com.robert.test.performance

import com.robert.Env

fun main() {
    when (Env.get("MODE", "GENERATOR")) {
        "GENERATOR" -> LoadGenerator().execute()
        else -> LoadTest().execute()
    }
}
