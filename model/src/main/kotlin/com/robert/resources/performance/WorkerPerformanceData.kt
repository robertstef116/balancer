package com.robert.resources.performance

data class WorkerPerformanceData(
    val logicalProcessorCount: Int,
    val cpuLoad: Double,
    val availableMemory: Long,
    val totalMemory: Long
)
