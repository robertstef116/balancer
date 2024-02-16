package com.robert.scaller

import com.robert.enums.LBAlgorithms
import java.util.UUID

data class WorkflowR(
    val id: UUID,
    val image: String,
    val memoryLimit: Long?,
    val cpuLimit: Long?,
    var minDeployments: Int?,
    var maxDeployments: Int?,
    var algorithm: LBAlgorithms,
    val pathsMapping: MutableMap<String, Int>
)
