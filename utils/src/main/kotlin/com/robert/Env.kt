package com.robert

import java.util.function.Function

object Env {
    private val log by logger()

    fun <T> get(name: String, fallback: T?, secret: Boolean, parser: Function<String, T>): T {
        val value = System.getenv(name)
        if (value != null) {
            return try {
                val parsedValue = parser.apply(value)
                log.info("Key found in environment '{} = {}'", name, if (secret) "*****" else parsedValue)
                parsedValue
            } catch (e: Exception) {
                if (fallback != null) {
                    log.error("Could not parse key '{}', replacing with fallback '{}'", name, fallback)
                    fallback
                } else {
                    throw UndefinedVariableExceptions(name)
                }
            }
        }
        if (fallback != null) {
            log.info("Key '{}' not set, replacing with fallback '{}'", name, fallback)
        } else {
            throw UndefinedVariableExceptions(name)
        }
        return fallback
    }

    fun getSecret(name: String, fallback: String? = null): String {
        val splits = getSplits(name)
        return get(splits[0] ?: name, splits[1] ?: fallback, true) { s: String -> s }
    }

    fun get(name: String, fallback: String? = null): String {
        val splits = getSplits(name)
        return get(splits[0] ?: name, splits[1] ?: fallback, false) { s: String -> s }
    }

    fun getLong(name: String, fallback: Long? = null): Long {
        val splits = getSplits(name)
        return get(splits[0] ?: name, splits[1]?.toLong() ?: fallback, false) { s: String -> s.toLong() }
    }

    fun getInt(name: String, fallback: Int? = null): Int {
        val splits = getSplits(name)
        return get(splits[0] ?: name, splits[1]?.toInt() ?: fallback, false) { s: String -> s.toInt() }
    }

    fun getDouble(name: String, fallback: Double? = null): Double {
        val splits = getSplits(name)
        return get(splits[0] ?: name, splits[1]?.toDouble() ?: fallback, false) { s: String -> s.toDouble() }
    }

    private fun getSplits(name: String): List<String?> {
        val varName = name.replaceFirst("^\\$\\{(.*)}".toRegex(), "$1")
        val parts = varName.split(":")
        return if (parts.size > 1) {
            parts
        } else mutableListOf(null, null)
    }
}
