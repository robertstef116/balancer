package com.robert

data class Deployment(
    val id: String,
    val workerId: String,
    val workflowId: String,
    val timestamp: Long,
    val portsMapping: Map<Int, Int>
)
