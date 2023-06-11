package com.robert.resources.performance

import java.util.*

abstract class PerformanceData {
    companion object {
        // TODO: dynamic
        private val metricsStoredLimit = 100
        @JvmStatic
        protected val memoryWeight = 0.5
        @JvmStatic
        protected val cpuWeight = 0.5

        private fun <T> addToBoundedQueue(queue: Queue<T>, value: T): MutableList<T> {
            val removedValues = mutableListOf<T>()
            while (queue.size > metricsStoredLimit) {
                removedValues.add(queue.remove())
            }
            queue.add(value)
            return removedValues
        }

        fun <T : PerformanceData> pickRandomResource(resources: Collection<T>): T? {
            val totalCpu = resources.sumOf { it.averageCpu }
            val totalMemory = resources.sumOf { it.averageMemory }
            var weight = kotlin.random.Random.nextDouble()
            for (resource in resources) {
                val resourceWeight = (1 - resource.averageCpu / totalCpu) * cpuWeight + (1 - resource.averageMemory / totalMemory) * memoryWeight
                if (weight < resourceWeight) {
                    return resource
                }
                weight -= resourceWeight
            }
            return null
        }
    }

    private val latestCpuData = LinkedList<Double>()
    private val latestMemoryData = LinkedList<Long>()
    protected var averageCpu = 0.0
    protected var averageMemory = 0.0

    fun addCpuLoadData(value: Double) {
        val cpuValue = 100 - value
        val removedValues = addToBoundedQueue(latestCpuData, cpuValue)
        averageCpu = ((averageCpu * latestCpuData.size) - removedValues.sum() + cpuValue) / (latestCpuData.size - removedValues.size + 1)
    }

    fun addMemoryData(value: Long) {
        val removedValues = addToBoundedQueue(latestMemoryData, value)
        averageMemory = ((averageMemory * latestMemoryData.size) - removedValues.sum() + value) / (latestMemoryData.size - removedValues.size + 1)
    }
}
