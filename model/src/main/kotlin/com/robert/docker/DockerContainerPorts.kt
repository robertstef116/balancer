package com.robert.docker

data class DockerContainerPorts(
    val deploymentId: String,
    val ports: Map<Int, Int>,
)
