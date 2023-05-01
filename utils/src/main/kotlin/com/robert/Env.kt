package com.robert

import org.slf4j.LoggerFactory
import java.util.function.Function

object Env {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun get(name: String): String {
        val varName = name.replaceFirst("^\\$\\{(.*)}".toRegex(), "$1")
        val parts = varName.split(":")
        return if (parts.size > 1) {
            get(parts[0], parts[1])
        } else System.getenv(varName)
    }

    fun get(name: String, fallback: String): String {
        val value = System.getenv(name)
        if (value != null) {
            log.info("key found in environment {} = {}", name, value)
            return value
        }
        log.info("key {} not set, replacing with fallback {}", name, fallback)
        return fallback
    }

    operator fun <T> get(name: String, fallback: T, parser: Function<String, T>): T {
        val value = System.getenv(name)
        if (value != null) {
            return try {
                val parsedValue = parser.apply(value)
                log.info("Key found in environment {} = {}", name, parsedValue)
                parsedValue
            } catch (e: Exception) {
                log.error("Could not parse key {}", name)
                fallback
            }
        }
        log.info("Key {} not set, replacing with fallback {}", name, fallback)
        return fallback
    }

    operator fun get(name: String, fallback: Long): Long {
        return get(name, fallback) { s: String -> s.toLong() }
    }

    operator fun get(name: String, fallback: Int): Int {
        return get(name, fallback) { s: String -> s.toInt() }
    }
}
