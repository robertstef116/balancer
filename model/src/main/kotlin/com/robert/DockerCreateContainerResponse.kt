package com.robert

data class DockerCreateContainerResponse(
    val id: String,
    val ports: Map<Int, Int>
)
