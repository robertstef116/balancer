package com.robert

data class WorkflowAnalyticsEntry(
    val workflowId: String,
    val image: String,
    val numberOfDeployments: Int,
    val timestamp: Long
)
