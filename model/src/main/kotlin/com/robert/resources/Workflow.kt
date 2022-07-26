package com.robert.resources

import com.robert.enums.LBAlgorithms

data class Workflow(
    val id: String,
    val image: String,
    val memoryLimit: Long?,
    val minDeployments: Int?,
    val maxDeployments: Int?,
    val upScaling: Int?,
    val downScaling: Int?,
    val algorithm: LBAlgorithms,
    val pathsMapping: Map<String, Int>
)
