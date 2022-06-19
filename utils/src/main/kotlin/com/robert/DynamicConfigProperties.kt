package com.robert

import org.slf4j.LoggerFactory

object DynamicConfigProperties {
    private val log = LoggerFactory.getLogger(DynamicConfigProperties::class.java)

    @JvmStatic
    private lateinit var properties: Map<String, String>

    @JvmStatic
    fun setPropertiesData(properties: Map<String, String>) {
        log.debug("properties updated")
        this.properties = properties
    }

    @JvmStatic
    fun getProperty(key: String): String? {
        val value = properties[key]
        log.trace("get property {} = {}", key, value)
        return value
    }

    @JvmStatic
    fun getIntProperty(key: String): Int? {
        val value = properties[key]?.toInt()
        log.trace("get property {} = {}", key, value)
        return value
    }

    @JvmStatic
    fun getIntPropertyOrDefault(key: String, default: Int): Int {
        val value = properties[key]?.toInt() ?: default
        log.trace("get property {} = {}", key, value)
        return value
    }

    @JvmStatic
    fun getLongPropertyOrDefault(key: String, default: Long): Long {
        val value = properties[key]?.toLong() ?: default
        log.trace("get property {} = {}", key, value)
        return value
    }

    @JvmStatic
    fun getFloatPropertyOrDefault(key: String, default: Float): Float {
        val value = properties[key]?.toFloat() ?: default
        log.trace("get property {} = {}", key, value)
        return value
    }
}
