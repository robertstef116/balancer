package com.robert.api.service

import com.robert.analytics.AnalyticsData
import com.robert.balancing.LoadBalancerResponseType
import com.robert.exceptions.ValidationException
import com.robert.storage.repository.AnalyticsRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class AnalyticsService : KoinComponent {
    private val analyticsRepository: AnalyticsRepository by inject()

    fun getScalingAnalytics(workflowId: UUID?, metric: String, durationMs: Long): List<AnalyticsData> {
        if (metric != "replicas" && metric != "avg_cpu" && metric != "avg_memory") {
            throw ValidationException("Unknown metric")
        }
        return analyticsRepository.getScalingAnalyticsData(workflowId, metric, durationMs)
    }

    fun getLoadBalancingAnalytics(workflowId: UUID?, path: String?, responseType: LoadBalancerResponseType, metric: String, durationMs: Long): List<AnalyticsData> {
        if (metric != "avg_response_time" && metric != "requests_count") {
            throw ValidationException("Unknown metric")
        }
        return analyticsRepository.getLoadBalancingAnalyticsData(workflowId, path, responseType, metric, durationMs)
    }
}
