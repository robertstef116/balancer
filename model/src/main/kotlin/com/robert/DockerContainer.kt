package com.robert

data class DockerContainer(
    val id: String,
    val name: String,
    val image: String,
    val created: Long,
    val status: String,
    val ports: Map<Int, Int>
)
