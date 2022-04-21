package com.robert

data class DeploymentCreateRequest (
    val path: String,
    val image: String,
    val memoryLimit: Long?,
    val ports: List<Int>?
)
