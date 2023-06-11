package com.robert.scaller

import com.robert.enums.LBAlgorithms
import java.util.UUID

data class WorkflowR(
    val id: UUID,
    val image: String,
    val memoryLimit: Long?,
    val cpuLimit: Long?,
    val minDeployments: Int?,
    val maxDeployments: Int?,
    val algorithm: LBAlgorithms,
    val pathsMapping: MutableMap<String, Int>
)
