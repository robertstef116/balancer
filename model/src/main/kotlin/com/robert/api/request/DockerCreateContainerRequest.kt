package com.robert.api.request

import java.util.UUID

data class DockerCreateContainerRequest(
    val deploymentId: UUID,
    val image: String,
    val memoryLimit: Long?, // in b
    val ports: List<Int>
)
