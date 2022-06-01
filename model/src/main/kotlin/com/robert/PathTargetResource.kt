package com.robert

data class PathTargetResource (
    val workerId: String,
    val workflowId: String,
    val deploymentId: String,
    val host: String,
    val port: Int,
)
