package com.robert

data class DockerCreateContainerRequest(
    val image: String,
    val memoryLimit: Long?, // in b
    val ports: List<Int>
)
