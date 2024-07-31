package com.robert.model

import com.robert.resources.DockerContainer
import com.robert.enums.WorkerState
import java.util.*

data class InternalWorkerStatus(
    val id: UUID,
    val host: String,
    var cpuLoad: Double,
    var memoryLoad: Double,
    var availableMemory: Long,
    var lastUpdate: Long,
    var state: WorkerState,
    var activeDeployments: List<DockerContainer>
)