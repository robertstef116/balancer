package com.robert.api.request

import com.robert.enums.LBAlgorithms

data class WorkflowCreateRequest (
    val image: String,
    val memoryLimit: Long?,
    val minDeployments: Int?,
    val maxDeployments: Int?,
    val upScaling: Int?,
    val downScaling: Int?,
    val algorithm: LBAlgorithms,
    val pathMapping: Map<String, Int>
)
