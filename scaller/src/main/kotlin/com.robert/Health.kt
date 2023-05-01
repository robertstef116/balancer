package com.robert

import org.slf4j.LoggerFactory
import java.util.LinkedList

class Health {
    companion object {
        private val LOG = LoggerFactory.getLogger(this::class.java)

        private val maxFailures = Env[Constants.HEALTH_CHECK_MAX_FAILURES, 3]
        private val metricsStoredLimit = Env[Constants.METRICS_STORED_PER_WORKER_LIMIT, 100]
    }

    private val latestAvailableCpuData = LinkedList<Double>()
    private val latestAvailableMemoryData = LinkedList<Long>()
    private var failures = 0

    fun increaseFailures() {
        failures++
    }

    fun resetFailures() {
        failures = 0
    }
}
