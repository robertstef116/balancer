package com.robert

data class DockerCreateContainerRequest(
    val deploymentId: String,
    val image: String,
    val memoryLimit: Long?, // in b
    val ports: List<Int>
)
