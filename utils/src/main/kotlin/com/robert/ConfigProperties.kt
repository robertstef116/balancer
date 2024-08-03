package com.robert

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.ktor.util.logging.*
import java.util.function.Function

object ConfigProperties {
    val LOG by logger()
    private val config: Config by lazy {
        ConfigFactory.load()
    }

    fun <T> get(key: String, fallback: T?, getter: Function<String, T>): T {
        return try {
            val value = getter.apply(key)
            LOG.info("Key found in environment {} = {}", key, value)
            value
        } catch (e: Exception) {
            if (fallback != null) {
                LOG.info("Key {} not set, replacing with fallback {}", key, fallback)
                fallback
            } else {
                LOG.error("Key {} not set and no fallback available", key)
                throw e
            }
        }
    }

    fun getString(key: String, default: String? = null): String {
        return get(key, default, config::getString)
    }

    fun getInteger(key: String, default: Int? = null): Int {
        return get(key, default, config::getInt)
    }
}
