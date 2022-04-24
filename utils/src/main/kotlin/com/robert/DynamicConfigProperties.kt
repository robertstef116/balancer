package com.robert

object DynamicConfigProperties {
    private lateinit var properties: Map<String, String>

    fun setPropertiesData(properties: Map<String, String>) {
        this.properties = properties
    }

    fun getProperty(key: String): String? = properties[key]
    fun getIntProperty(key: String): Int? = properties[key]?.toInt()
}
