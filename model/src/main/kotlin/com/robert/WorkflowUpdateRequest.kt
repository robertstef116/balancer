package com.robert

data class WorkflowUpdateRequest(
    val minDeployments: Int?,
    val maxDeployments: Int?,
    val algorithm: LBAlgorithms
)
