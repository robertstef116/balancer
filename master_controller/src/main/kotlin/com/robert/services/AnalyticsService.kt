package com.robert.services

import com.robert.analytics.UsageAnalyticsEntry
import com.robert.analytics.ImageScalingAnalyticsData
import com.robert.analytics.WorkflowAnalyticsEntry
import com.robert.enums.WorkflowAnalyticsEventType
import com.robert.persistance.AnalyticsStorage
import java.util.LinkedList

class AnalyticsService {
    private val analyticsStorage = AnalyticsStorage()

    fun getAnalytics(from: String?, workerId: String?, workflowId: String?, deploymentId: String?): List<UsageAnalyticsEntry> {
        if (from == null) {
            return emptyList()
        }

        return analyticsStorage.getAnalytics(from.toLong(), workerId, workflowId, deploymentId)
    }

    fun getWorkflowAnalytics(from: String?, workerId: String?, workflowId: String?):  Pair<Map<String, ImageScalingAnalyticsData>, List<WorkflowAnalyticsEntry>> {
        if (from == null) {
            return Pair(mapOf(), emptyList())
        }

        val (workflowMapping, analyticsData) = analyticsStorage.getWorkflowAnalytics(from.toLong(), workerId, workflowId)
        val workflowCurrentScaling = mutableMapOf<String, ImageScalingAnalyticsData>()
        val workflowAnalytics = LinkedList<WorkflowAnalyticsEntry>()

        for((key, imageScalingData) in workflowMapping) {
            workflowCurrentScaling[key] = ImageScalingAnalyticsData(imageScalingData.image, imageScalingData.numberOfDeployments)
        }

        for (data in analyticsData.reversed()) {
            var singleMapping = workflowMapping[data.workflowId]
            if (singleMapping == null) {
                singleMapping = ImageScalingAnalyticsData("unknown", 0)
                workflowMapping[data.workflowId] = singleMapping
            }
            var numberOfDeployments = singleMapping.numberOfDeployments
            if (data.event == WorkflowAnalyticsEventType.ADD) {
                numberOfDeployments = maxOf(numberOfDeployments - 1, 0)
            } else {
                numberOfDeployments += 1
            }
            workflowMapping[data.workflowId]!!.numberOfDeployments = numberOfDeployments
            workflowAnalytics.addFirst(WorkflowAnalyticsEntry(data.workflowId, data.image, numberOfDeployments, data.timestamp))
        }

        return Pair(workflowCurrentScaling, workflowAnalytics)
    }
}
