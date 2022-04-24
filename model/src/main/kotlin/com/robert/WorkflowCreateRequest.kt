package com.robert

data class WorkflowCreateRequest (
    val image: String,
    val memoryLimit: Long?,
    val algorithm: LBAlgorithms,
    val pathMapping: Map<String, Int>
)
