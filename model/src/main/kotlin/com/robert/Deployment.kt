package com.robert

data class Deployment(
    val id: String,
    val path: String,
    val image: String,
    val memoryLimit: Long?,
    val ports: List<Int>
)
