package com.robert

import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read

data class DeploymentPerformance(
    val deploymentId: String,
    val containerId: String,
) {
    val resourcesLock = ReentrantReadWriteLock()
    val latestAvailableCpus = LinkedList<Double>()
    val latestAvailableMemories = LinkedList<Long>()

    fun getAverageAvailableCpu(): Double {
        resourcesLock.read {
            return latestAvailableCpus.average()
        }
    }

    fun getAverageAvailableMemory(): Double {
        resourcesLock.read {
            return latestAvailableMemories.average()
        }
    }
}
