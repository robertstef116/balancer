package com.robert.scaling.client.model

import java.util.*

data class DeploymentScalingRequest(
    val containerId: String?,
    val workflowId: UUID?,
    val image: String?,
    val exposedPorts: List<Int>?,
    val cpuLimit: Long?,
    val memoryLimit: Long?,
    val type: Type,
    var registered: Boolean
) {
    enum class Type {
        UP,
        DOWN
    }
}
