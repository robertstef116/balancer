package com.robert.scaller

import java.util.UUID

data class ScalingAnalytic (
    val workflowId: UUID,
    val replicas: Int,
    val avgCpu: Double,
    val avgMemory: Double,
    val timestamp: Long,
)