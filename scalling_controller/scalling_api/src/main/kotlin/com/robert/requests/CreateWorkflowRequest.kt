package com.robert.requests

import com.robert.enums.LBAlgorithms

data class CreateWorkflowRequest (
    val image: String,
    val memoryLimit: Long?,
    val cpuLimit: Long?,
    val minDeployments: Int?,
    val maxDeployments: Int?,
    val algorithm: LBAlgorithms,
    val pathsMapping: MutableMap<String, Int>
)
