package com.robert

import java.util.UUID

object Constants {
    @JvmField
    val HASH = UUID.randomUUID().toString()

    // Static config properties keys
    const val MANAGED_CONTAINER_LABEL = "MANAGED_BY_BALANCER"

    // Dynamic config properties keys
    const val PROCESSING_SOCKET_BUFFER_LENGTH = "PROCESSING_SOCKET_BUFFER_LENGTH"
    const val COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL = "COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL"

    // Update aware services keys
    const val CONFIG_SERVICE_KEY = "CONFIG_SERVICE"
    const val WORKER_SERVICE_KEY = "WORKER_SERVICE"
    const val WORKFLOW_SERVICE_KEY = "WORKFLOW_SERVICE"
}
