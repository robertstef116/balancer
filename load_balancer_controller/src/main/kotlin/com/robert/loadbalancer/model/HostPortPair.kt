package com.robert.loadbalancer.model

import java.util.UUID

data class HostPortPair(
    val workflowId: UUID,
    val host: String,
    val port: Int,
)