package com.robert

data class Workflow(
    val id: String,
    val image: String,
    val memoryLimit: Long?,
    val minDeployments: Int?,
    val maxDeployments: Int?,
    val algorithm: LBAlgorithms,
    val pathsMapping: Map<String, Int>
)
