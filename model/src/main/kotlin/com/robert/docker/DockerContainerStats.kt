package com.robert.docker

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class DockerContainerStats(
    @Contextual
    val deploymentId: UUID,
    val containerId: String,
    val cpuLoad: Double,
    val availableMemory: Long,
    val totalMemory: Long
)
