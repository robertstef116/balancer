package com.robert.services

import com.robert.AnalyticsEntry
import com.robert.ImageScalingData
import com.robert.WorkflowAnalyticsData
import com.robert.WorkflowAnalyticsEntry
import com.robert.exceptions.WorkflowAnalyticsEvent
import com.robert.persistance.AnalyticsStorage
import java.util.LinkedList

class AnalyticsService {
    private val analyticsStorage = AnalyticsStorage()

    fun getAnalytics(from: String?, workerId: String?, workflowId: String?, deploymentId: String?): List<AnalyticsEntry> {
        if (from == null) {
            return emptyList()
        }

        return analyticsStorage.getAnalytics(from.toLong(), workerId, workflowId, deploymentId)
    }

    fun getWorkflowAnalytics(from: String?, workerId: String?, workflowId: String?):  Pair<Map<String, ImageScalingData>, List<WorkflowAnalyticsEntry>> {
        if (from == null) {
            return Pair(mapOf(), emptyList())
        }

        val (workflowMapping, analyticsData) = analyticsStorage.getWorkflowAnalytics(from.toLong(), workerId, workflowId)
        val workflowCurrentScaling = mutableMapOf<String, ImageScalingData>()
        val workflowAnalytics = LinkedList<WorkflowAnalyticsEntry>()

        println(workflowMapping)

        for((key, imageScalingData) in workflowMapping) {
            workflowCurrentScaling[key] = ImageScalingData(imageScalingData.image, imageScalingData.numberOfDeployments)
        }

        for (data in analyticsData.reversed()) {
            var singleMapping = workflowMapping[data.workflowId]
            if (singleMapping == null) {
                singleMapping = ImageScalingData("unknown", 0)
                workflowMapping[data.workflowId] = singleMapping
            }
            var numberOfDeployments = singleMapping.numberOfDeployments
            if (data.event == WorkflowAnalyticsEvent.ADD) {
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
