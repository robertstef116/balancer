package com.robert

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object ConfigProperties {
    private val config: Config = ConfigFactory.load()

    fun getString(key: String): String? {
        return try {
            config.getString(key)
        } catch (_: Exception) {
            null
        }
    }

    fun getInteger(key: String): Int? {
        return try {
            config.getInt(key)
        } catch (_: Exception) {
            null
        }
    }
}
