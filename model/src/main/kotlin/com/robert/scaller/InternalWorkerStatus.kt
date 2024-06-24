package com.robert.scaller

import com.robert.docker.DockerContainer
import java.util.UUID

data class InternalWorkerStatus(
    val id: UUID,
    var cpuLoad: Double,
    var memoryLoad: Double,
    var availableMemory: Long,
    var lastUpdate: Long,
    var activeDeployments: List<DockerContainer>
)