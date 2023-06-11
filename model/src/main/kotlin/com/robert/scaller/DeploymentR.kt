package com.robert.scaller

import java.util.UUID

data class DeploymentR (
    val id: UUID,
    val workerId: UUID,
    val workflowId: UUID,
    val containerId: String,
    val portsMapping: MutableMap<Int, Int>
)
