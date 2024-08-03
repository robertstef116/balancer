package com.robert.storage.repository

import com.robert.analytics.AnalyticsData
import com.robert.balancing.LoadBalancerResponseType
import java.util.*

interface AnalyticsRepository {
    fun getScalingAnalyticsData(workflowId: UUID?, metric: String, durationMs: Long): List<AnalyticsData>
    fun getLoadBalancingAnalyticsData(workflowId: UUID?, path: String?, responseType: LoadBalancerResponseType, metric: String, durationMs: Long): List<AnalyticsData>
}