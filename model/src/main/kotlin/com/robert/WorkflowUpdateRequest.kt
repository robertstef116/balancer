package com.robert

data class WorkflowUpdateRequest(
    val path: String?,
    val image: String?,
    val memoryLimit: Long?,
    val ports: List<Int>?
)
