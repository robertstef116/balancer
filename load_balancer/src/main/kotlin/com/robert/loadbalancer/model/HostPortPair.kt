package com.robert.loadbalancer.model

import java.util.*

data class HostPortPair(
    val workflowId: UUID,
    val host: String,
    val port: Int,
)