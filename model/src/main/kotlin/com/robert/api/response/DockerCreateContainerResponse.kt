package com.robert.api.response

data class DockerCreateContainerResponse(
    val id: String,
    val ports: Map<Int, Int>
)
