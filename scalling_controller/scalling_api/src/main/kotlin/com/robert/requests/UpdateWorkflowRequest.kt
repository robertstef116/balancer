package com.robert.requests

import com.robert.enums.LBAlgorithms
import java.util.*

data class UpdateWorkflowRequest(
    val id: UUID,
    val minDeployments: Int?,
    val maxDeployments: Int?,
    val algorithm: LBAlgorithms?,
)
