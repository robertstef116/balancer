package com.robert.api.model.workflow

import com.robert.enums.LBAlgorithms

data class WorkflowCreateRequest (
    val image: String,
    val memoryLimit: Long,
    val cpuLimit: Long,
    val minDeployments: Int?,
    val maxDeployments: Int?,
    val algorithm: LBAlgorithms,
    val pathMapping: Map<String, Int>
)
