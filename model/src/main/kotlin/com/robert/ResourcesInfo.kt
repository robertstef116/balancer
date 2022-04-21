package com.robert

data class ResourcesInfo(
    val logicalProcessorCount: Int,
    val cpuLoad: Double,
    val availableMemory: Long,
    val totalMemory: Long
)
