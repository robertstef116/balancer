package com.robert.balancing

data class RequestTargetData (
    val workerId: String,
    val workflowId: String,
    val deploymentId: String,
    val host: String,
    val port: Int,
)
