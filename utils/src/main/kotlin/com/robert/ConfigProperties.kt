package com.robert

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object ConfigProperties {
    private val config: Config = ConfigFactory.load()

    fun getString(key: String): String = config.getString(key)
    fun getInteger(key: String): Int = config.getInt(key)
}
