package com.robert

data class WorkflowCreateRequest (
    val path: String,
    val image: String,
    val memoryLimit: Long?,
    val ports: List<Int>?
)
