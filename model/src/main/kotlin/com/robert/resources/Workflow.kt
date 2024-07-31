package com.robert.resources

import com.robert.enums.LBAlgorithms
import java.util.*

data class Workflow(
    val id: UUID,
    val image: String,
    val cpuLimit: Long,
    val memoryLimit: Long,
    var minDeployments: Int?,
    var maxDeployments: Int?,
    var algorithm: LBAlgorithms,
    val pathsMapping: Map<String, Int>
)
