package com.robert.services

import com.robert.AnalyticsEntry
import com.robert.persistance.AnalyticsStorage

class AnalyticsService {
    private val analyticsStorage = AnalyticsStorage()

    fun getAnalytics(from: String?, workerId: String?, workflowId: String?, deploymentId: String?): List<AnalyticsEntry> {
        if (from == null) {
            return emptyList()
        }

        return analyticsStorage.getAnalytics(from.toLong(), workerId, workflowId, deploymentId)
    }
}
