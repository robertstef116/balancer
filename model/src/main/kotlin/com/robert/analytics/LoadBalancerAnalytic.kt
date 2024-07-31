package com.robert.analytics

import com.robert.balancing.LoadBalancerResponseType
import java.util.*

data class LoadBalancerAnalytic(
    val workflowId: UUID,
    val path: String,
    val responseTime: Long,
    val timestamp: Long,
    val responseType: LoadBalancerResponseType,
)
