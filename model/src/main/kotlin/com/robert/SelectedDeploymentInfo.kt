package com.robert

data class SelectedDeploymentInfo(
    val host: String,
    val port: Int,
    val referenceId: String,
    val targetResource: PathTargetResource
)
