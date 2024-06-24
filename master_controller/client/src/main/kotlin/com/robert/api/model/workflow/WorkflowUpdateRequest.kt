package com.robert.api.model.workflow

import com.robert.enums.LBAlgorithms

data class WorkflowUpdateRequest(
    val minDeployments: Int?,
    val maxDeployments: Int?,
    val algorithm: LBAlgorithms
)
