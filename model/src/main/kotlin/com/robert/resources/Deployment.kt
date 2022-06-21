package com.robert.resources

data class Deployment(
    val id: String,
    val workerId: String,
    val workflowId: String,
    val containerId: String,
    val timestamp: Long,
    val portsMapping: Map<Int, Int>
)
