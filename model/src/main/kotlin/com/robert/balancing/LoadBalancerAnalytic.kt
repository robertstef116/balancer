package com.robert.balancing

import java.util.UUID

data class LoadBalancerAnalytic(
    val workflowId: UUID,
    val path: String,
    val responseTime: Long,
    val timestamp: Long,
    val responseType: LoadBalancerResponseType,
)
