package com.robert.scaller

import java.util.*

data class InternalDeploymentStatus(
    val id: UUID,
    val workflowId: UUID,
    val workerId: UUID,
    var score: Double,
    val port: Int,
)
