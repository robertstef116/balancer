package com.robert.docker

import java.util.*

data class DockerContainer(
    val containerID: String,
    val workflowId: UUID,
    val cpuUsage: Double,
    val memoryUsage: Double,
    val ports: List<DockerPortMapping>
)

data class DockerPortMapping(
    val publicPort: Int,
    val privatePort: Int,
)
