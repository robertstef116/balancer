package com.robert

data class DeploymentUpdateRequest(
    val path: String?,
    val image: String?,
    val memoryLimit: Long?,
    val ports: List<Int>?
)
