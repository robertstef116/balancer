package com.robert.analytics

import com.robert.enums.WorkflowAnalyticsEventType

data class WorkflowAnalyticsData(
    val workflowId: String,
    val image: String,
    val event: WorkflowAnalyticsEventType,
    val timestamp: Long
)
