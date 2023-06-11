package com.robert

import org.slf4j.LoggerFactory
import java.util.function.Function

object Env {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun <T> get(name: String, fallback: T?, parser: Function<String, T>): T? {
        val value = System.getenv(name)
        if (value != null) {
            return try {
                val parsedValue = parser.apply(value)
                log.info("Key found in environment {} = {}", name, parsedValue)
                parsedValue
            } catch (e: Exception) {
                log.error("Could not parse key {}, replacing with fallback {}", name, fallback)
                fallback
            }
        }
        log.info("Key {} not set, replacing with fallback {}", name, fallback)
        return fallback
    }

    fun get(name: String, fallback: String? = null): String {
        val splits = getSplits(name)
        return get(splits[0] ?: name, splits[1] ?: fallback) { s: String -> s }?: throw UndefinedVariableExceptions(name)
    }

    fun getLong(name: String, fallback: Long? = null): Long {
        val splits = getSplits(name)
        return get(splits[0] ?: name, splits[1]?.toLong() ?: fallback) { s: String -> s.toLong() }?: throw UndefinedVariableExceptions(name)
    }

    fun getInt(name: String, fallback: Int? = null): Int {
        val splits = getSplits(name)
        return get(splits[0] ?: name, splits[1]?.toInt() ?: fallback) { s: String -> s.toInt() }?: throw UndefinedVariableExceptions(name)
    }

    private fun getSplits(name: String): List<String?> {
        val varName = name.replaceFirst("^\\$\\{(.*)}".toRegex(), "$1")
        val parts = varName.split(":")
        return if (parts.size > 1) {
            parts
        } else mutableListOf(null, null)
    }
}
