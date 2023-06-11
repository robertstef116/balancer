package com.robert.resources.performance

import kotlinx.serialization.Serializable

@Serializable
data class WorkerPerformanceData(
    val logicalProcessorCount: Int,
    val cpuLoad: Double,
    val availableMemory: Long,
    val totalMemory: Long
)
