package com.robert.scaller

import java.util.UUID

data class ScalingAnalytic (
    val workflowId: UUID,
    val replicas: Int,
    val timestamp: Long,
)