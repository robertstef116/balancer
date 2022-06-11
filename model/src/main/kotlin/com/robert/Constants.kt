package com.robert

import java.util.UUID

object Constants {
    // Static config properties keys
    const val MANAGED_CONTAINER_LABEL = "MANAGED_BY_BALANCER"
    const val DEPLOYMENT_ID_KEY_LABEL = "DEPLOYMENT_ID"

    // Dynamic config properties keys
    const val PROCESSING_SOCKET_BUFFER_LENGTH = "PROCESSING_SOCKET_BUFFER_LENGTH"
    const val COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL = "COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL"
    const val HEALTH_CHECK_TIMEOUT = "HEALTH_CHECK_TIMEOUT"
    const val HEALTH_CHECK_INTERVAL = "HEALTH_CHECK_INTERVAL"
    const val HEALTH_CHECK_MAX_FAILURES = "HEALTH_CHECK_MAX_FAILURES"
    const val CPU_WEIGHT = "CPU_WEIGHT"
    const val MEMORY_WEIGHT = "MEMORY_WEIGHT"
    const val DEPLOYMENTS_CHECK_INTERVAL = "DEPLOYMENTS_CHECK_INTERVAL"
    const val MASTER_CHANGES_CHECK_INTERVAL = "MASTER_CHANGES_CHECK_INTERVAL"
    const val NUMBER_RELEVANT_PERFORMANCE_METRICS = "NUMBER_RELEVANT_PERFORMANCE_METRICS"

    // Update aware services keys
    const val UPDATE_AWARE_SERVICE_SUFFIX = "_CHANGE_TIMESTAMP"
    const val CONFIG_SERVICE_KEY = "CONFIG_SERVICE$UPDATE_AWARE_SERVICE_SUFFIX"
    const val WORKER_SERVICE_KEY = "WORKER_SERVICE$UPDATE_AWARE_SERVICE_SUFFIX"
    const val WORKFLOW_SERVICE_KEY = "WORKFLOW_SERVICE$UPDATE_AWARE_SERVICE_SUFFIX"

    // Default values
    const val DEFAULT_COMPUTE_WEIGHTED_RESPONSE_TIME_INTERVAL = 10
}
