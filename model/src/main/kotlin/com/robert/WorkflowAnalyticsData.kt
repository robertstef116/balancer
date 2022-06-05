package com.robert

import com.robert.exceptions.WorkflowAnalyticsEvent

data class WorkflowAnalyticsData(
    val workflowId: String,
    val image: String,
    val event: WorkflowAnalyticsEvent,
    val timestamp: Long
)
