package com.robert

data class WorkflowUpdateRequest(
    val memoryLimit: Long?,
    val algorithm: LBAlgorithms?
)
