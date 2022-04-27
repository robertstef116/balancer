package com.robert

object DynamicConfigProperties {
    private lateinit var properties: Map<String, String>

    fun setPropertiesData(properties: Map<String, String>) {
        this.properties = properties
    }

    fun getProperty(key: String): String? = properties[key]
    fun getIntProperty(key: String): Int? = properties[key]?.toInt()
    fun getIntPropertyOrDefault(key: String, default: Int): Int = properties[key]?.toInt() ?: default
    fun getLongPropertyOrDefault(key: String, default: Long): Long = properties[key]?.toLong() ?: default
    fun getFloatPropertyOrDefault(key: String, default: Float): Float = properties[key]?.toFloat() ?: default
}
