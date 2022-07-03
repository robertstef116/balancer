package com.robert.balancing

data class TargetData (
    val workerId: String,
    val workflowId: String,
    val deploymentId: String,
    val host: String,
    val port: Int,
)
