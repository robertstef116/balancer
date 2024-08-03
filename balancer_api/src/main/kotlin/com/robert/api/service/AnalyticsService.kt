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

//    fun getAnalytics(from: String?, workerId: String?, workflowId: String?, deploymentId: String?): List<UsageAnalyticsEntry> {
//        if (from == null) {
//            return emptyList()
//        }
//
//        return analyticsStorage.getAnalytics(from.toLong(), workerId, workflowId, deploymentId)
//    }
//
//    fun getWorkflowAnalytics(from: String?, workerId: String?, workflowId: String?): Pair<Map<String, ImageScalingAnalyticsData>, List<WorkflowAnalyticsEntry>> {
//        if (from == null) {
//            return Pair(mapOf(), emptyList())
//        }
//
//        val (workflowMapping, analyticsData) = analyticsStorage.getWorkflowAnalytics(from.toLong(), workerId, workflowId)
//        val workflowCurrentScaling = mutableMapOf<String, ImageScalingAnalyticsData>()
//        val workflowAnalytics = LinkedList<WorkflowAnalyticsEntry>()
//
//        for ((key, imageScalingData) in workflowMapping) {
//            workflowCurrentScaling[key] = ImageScalingAnalyticsData(imageScalingData.image, imageScalingData.numberOfDeployments)
//        }
//
//        for (data in analyticsData.reversed()) {
//            var singleMapping = workflowMapping[data.workflowId]
//            if (singleMapping == null) {
//                singleMapping = ImageScalingAnalyticsData("unknown", 0)
//                workflowMapping[data.workflowId] = singleMapping
//            }
//            var numberOfDeployments = singleMapping.numberOfDeployments
//            if (data.event == WorkflowAnalyticsEventType.ADD) {
//                numberOfDeployments = maxOf(numberOfDeployments - 1, 0)
//            } else {
//                numberOfDeployments += 1
//            }
//            workflowMapping[data.workflowId]!!.numberOfDeployments = numberOfDeployments
//            workflowAnalytics.addFirst(WorkflowAnalyticsEntry(data.workflowId, data.image, numberOfDeployments, data.timestamp))
//        }
//
//        return Pair(workflowCurrentScaling, workflowAnalytics)
//    }
}
