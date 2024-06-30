package com.robert.scaling.client.model

import com.robert.enums.LBAlgorithms

data class WorkflowDeploymentData(
    val path: String,
    val host: String,
    val port: Int,
    val algorithm: LBAlgorithms,
    val score: Double,
)
