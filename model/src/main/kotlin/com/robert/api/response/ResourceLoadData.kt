package com.robert.api.response

import com.robert.enums.LBAlgorithms
import java.util.UUID

data class ResourceLoadData (
    val workflowId: UUID,
    val algorithm: LBAlgorithms,
    val utilization: List<Double>,
    val paths: Map<String, List<Int>>,
    val timestamp: Long
)
