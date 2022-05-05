package com.robert

data class DockerContainerStats(
    val deploymentId: String,
    val containerId: String,
    val cpuLoad: Double,
    val availableMemory: Long,
    val totalMemory: Long
)
