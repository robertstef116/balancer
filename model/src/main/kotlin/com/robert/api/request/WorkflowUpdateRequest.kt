package com.robert.api.request

import com.robert.enums.LBAlgorithms

data class WorkflowUpdateRequest(
    val minDeployments: Int?,
    val maxDeployments: Int?,
    val algorithm: LBAlgorithms
)
