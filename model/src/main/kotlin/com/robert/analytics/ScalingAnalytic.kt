package com.robert.analytics

import java.util.*

data class ScalingAnalytic(
    val workflowId: UUID,
    val replicas: Int,
    val avgCpu: Double,
    val avgMemory: Double,
    val timestamp: Long,
)